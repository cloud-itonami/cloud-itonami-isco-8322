(ns driving.governor
  "CarVanDrivingGovernor — the independent safety/traceability layer
  named in this repository's README/business-model.md, gating the
  robot-dispensed physical work (pre-trip inspection checks,
  cargo/passenger-area sensing) an advisor may propose. The governor
  never dispatches hardware itself. Modeled on
  cloud-itonami-isco-4311's bookkeeping.governor. Trip twist: a
  proposed trip's cumulative duty hours (already worked plus the
  trip) is arithmetic comparison against the registered daily ceiling
  — duty hours are cumulative arithmetic, not driver discretion — and
  days since last inspection must not exceed the registered validity
  window — a trip on an expired inspection is not a documented
  vehicle.

  HARD invariants (:hard? true, ALWAYS :hold, never overridable):
    1. client provenance — the organization must be registered.
    2. no-actuation      — proposal :effect must be :propose (the
                           governor never dispatches hardware; it only
                           gates what the robot may execute).
    3. driver basis         — a trip approval must cite a REGISTERED
                           driver belonging to this client.
    4. duty-hours ceiling   — hours-worked-today + trip-duration-hours
                           must not exceed the driver's registered
                           :max-daily-duty-hours (cumulative
                           arithmetic, not driver discretion).
    5. inspection validity  — the proposed days-since-inspection must
                           not exceed the driver's registered
                           :inspection-validity-days (a trip on an
                           expired inspection is not a documented
                           vehicle).
  ESCALATION invariants (:escalate? true, ALWAYS human sign-off per
  business-model.md's Trust Controls — these are :high/
  :safety-critical regardless of confidence):
    6. :op :approve-duty-hours-exception (no trip acceptance beyond
                           the duty-hours limit without the governor
                           gate).
    7. :op :approve-high-risk-trip (high-risk trip acceptance always
                           requires human sign-off).
    8. low confidence (< `confidence-floor`)."
  (:require [driving.store :as store]))

(def confidence-floor 0.6)

(def ^:private always-escalate-ops #{:approve-duty-hours-exception
                                     :approve-high-risk-trip})

(defn- hard-violations [{:keys [request proposal]} client-record d]
  (let [{:keys [op hours-worked-today trip-duration-hours days-since-inspection]} proposal
        trip? (= :approve-trip op)]
    (cond-> []
      (nil? client-record)
      (conj {:rule :no-client :detail "未登録 client"})

      (not= :propose (:effect proposal))
      (conj {:rule :no-actuation :detail "effect は :propose のみ許可（governor はハードウェアを直接起動しない）"})

      (and trip? (nil? d))
      (conj {:rule :unknown-driver :detail "未登録 driver への配車承認は不可"})

      (and trip? d (not= (:client-id d) (:client-id request)))
      (conj {:rule :driver-wrong-client :detail "driver が別 client のもの"})

      (and trip? d (number? hours-worked-today) (number? trip-duration-hours)
           (> (+ hours-worked-today trip-duration-hours) (:max-daily-duty-hours d)))
      (conj {:rule :duty-hours-exceeded
             :detail (str "累積勤務時間 " (+ hours-worked-today trip-duration-hours)
                          "h > 登録済み日次上限 " (:max-daily-duty-hours d)
                          "h（勤務時間は累積算術であって運転手の裁量ではない）")})

      (and trip? d (integer? days-since-inspection)
           (> days-since-inspection (:inspection-validity-days d)))
      (conj {:rule :inspection-expired
             :detail (str "点検後経過日数 " days-since-inspection " > 登録済み有効期間 "
                          (:inspection-validity-days d)
                          "（点検切れ車両での配車は書類の整った車両ではない）")}))))

(defn check
  "Assess a proposal against `request`/`context`/`proposal` and a
  `store` implementing `driving.store/Store`. Pure — never mutates the
  store, never dispatches the robot."
  [request context proposal store]
  (let [client-record (store/client store (:client-id request))
        d (some->> (:driver-id proposal) (store/driver store))
        hard (hard-violations {:request request :proposal proposal}
                              client-record d)
        hard? (boolean (seq hard))
        conf (or (:confidence proposal) 0.0)
        low? (< conf confidence-floor)
        always-risky? (contains? always-escalate-ops (:op proposal))]
    {:ok? (and (not hard?) (not low?) (not always-risky?))
     :violations hard
     :confidence conf
     :hard? hard?
     :escalate? (and (not hard?) (or low? always-risky?))}))
