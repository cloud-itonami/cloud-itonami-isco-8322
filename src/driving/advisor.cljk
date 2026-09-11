(ns driving.advisor
  "DrivingAdvisor — the advisor named in this repository's README,
  proposing a trip operation (approve a trip, approve a duty-hours
  exception, approve a high-risk trip) from a trip request,
  duty-hours log and vehicle-inspection record. Swappable mock/llm;
  the advisor ONLY proposes — `driving.governor` checks the duty-hours
  ceiling and inspection validity independently and always escalates
  duty-hours-exception/high-risk decisions. Modeled on
  cloud-itonami-isco-4311's advisor.

  A proposal: {:op :approve-trip|:approve-duty-hours-exception|:approve-high-risk-trip
               :effect :propose :driver-id str
               :hours-worked-today number :trip-duration-hours number
               :days-since-inspection int :stake kw :confidence n
               :rationale str}"
  (:require #?(:clj [clojure.edn :as edn] :cljs [cljs.reader :as edn])))

(defprotocol Advisor
  (-advise [advisor store request] "request -> proposal map"))

(defn- infer [_store {:keys [op stake driver-id hours-worked-today
                             trip-duration-hours days-since-inspection] :as request}]
  {:op op
   :effect :propose
   :driver-id driver-id
   :hours-worked-today hours-worked-today
   :trip-duration-hours trip-duration-hours
   :days-since-inspection days-since-inspection
   :stake (or stake :low)
   :confidence (case (or stake :low) :high 0.7 :medium 0.85 :low 0.95)
   :rationale (str "proposed " (name op) " for client " (:client-id request))})

(defn mock-advisor []
  (reify Advisor
    (-advise [_ store request] (infer store request))))

(def ^:private system-prompt
  "You are a car/van driving-practice advisor. Given a request,
   propose an :op, the :driver-id, :hours-worked-today,
   :trip-duration-hours and :days-since-inspection, an honest
   :confidence and a :stake. Never call an over-duty-hours trip or an
   expired-inspection trip conforming — the governor checks both
   against the registered driver record. Duty-hours-exception and
   high-risk-trip decisions always require human sign-off regardless
   of confidence.")

(defn- parse-proposal [content]
  (try
    (let [p (edn/read-string content)]
      (if (map? p)
        (assoc p :effect :propose)
        {:op :unknown :effect :propose :confidence 0.0 :stake :high
         :rationale "unparseable LLM response"}))
    (catch #?(:clj Exception :cljs js/Error) _
      {:op :unknown :effect :propose :confidence 0.0 :stake :high
       :rationale "LLM response parse failure"})))

(defn llm-advisor
  [chat-model model-generate-fn gen-opts]
  (reify Advisor
    (-advise [_ _store request]
      (let [msgs [{:role :system :content system-prompt}
                  {:role :user :content (str "operation request: " (pr-str request))}]
            resp (model-generate-fn chat-model msgs gen-opts)]
        (parse-proposal (:content resp))))))
