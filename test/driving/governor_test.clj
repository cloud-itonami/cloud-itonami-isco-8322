(ns driving.governor-test
  (:require [clojure.test :refer [deftest is testing]]
            [driving.store :as store]
            [driving.governor :as governor]))

(defn- fresh-store []
  (let [st (store/mem-store)]
    (store/register-client! st {:client-id "client-1" :name "Kobo Driving Co"})
    (store/register-driver! st {:driver-id "D-1" :client-id "client-1"
                                :name "route-van-3"
                                :max-daily-duty-hours 10
                                :inspection-validity-days 30})
    st))

(defn- trip [worked duration days]
  {:op :approve-trip :effect :propose :driver-id "D-1"
   :hours-worked-today worked :trip-duration-hours duration
   :days-since-inspection days :confidence 0.9 :stake :low})

(def ^:private req {:client-id "client-1"})

(deftest ok-within-duty-hours-and-inspection-valid
  (let [st (fresh-store)
        v (governor/check req {} (trip 5 2 10) st)]
    (is (:ok? v))))

(deftest ok-at-exact-duty-hours-and-inspection-edges
  (testing "both ceilings are inclusive"
    (let [st (fresh-store)]
      (is (:ok? (governor/check req {} (trip 8 2 30) st)))
      (is (:ok? (governor/check req {} (trip 5 2 30) st))))))

(deftest hard-on-duty-hours-exceeded
  (testing "duty hours are cumulative arithmetic, not driver discretion"
    (let [st (fresh-store)
          v (governor/check req {} (assoc (trip 9 3 10) :confidence 0.99) st)]
      (is (:hard? v))
      (is (some #(= :duty-hours-exceeded (:rule %)) (:violations v))))))

(deftest hard-on-inspection-expired
  (testing "a trip on an expired inspection is not a documented vehicle"
    (let [st (fresh-store)
          v (governor/check req {} (assoc (trip 5 2 60) :confidence 0.99) st)]
      (is (:hard? v))
      (is (some #(= :inspection-expired (:rule %)) (:violations v))))))

(deftest hard-on-unknown-driver
  (let [st (fresh-store)
        v (governor/check req {} (assoc (trip 5 2 10) :driver-id "D-ghost") st)]
    (is (:hard? v))
    (is (some #(= :unknown-driver (:rule %)) (:violations v)))))

(deftest hard-on-foreign-driver
  (let [st (fresh-store)]
    (store/register-client! st {:client-id "client-2" :name "Other"})
    (let [v (governor/check {:client-id "client-2"} {} (trip 5 2 10) st)]
      (is (:hard? v))
      (is (some #(= :driver-wrong-client (:rule %)) (:violations v))))))

(deftest hard-on-unregistered-client
  (let [st (fresh-store)
        v (governor/check {:client-id "nobody"} {} (trip 5 2 10) st)]
    (is (:hard? v))
    (is (some #(= :no-client (:rule %)) (:violations v)))))

(deftest hard-on-no-actuation-violation
  (let [st (fresh-store)
        v (governor/check req {} (assoc (trip 5 2 10) :effect :direct-write) st)]
    (is (:hard? v))
    (is (some #(= :no-actuation (:rule %)) (:violations v)))))

(deftest always-escalates-duty-hours-exception-even-at-high-confidence
  (testing "no trip acceptance beyond duty-hours limit without the governor gate"
    (let [st (fresh-store)
          v (governor/check req {} {:op :approve-duty-hours-exception :effect :propose
                                    :driver-id "D-1" :confidence 0.99 :stake :low} st)]
      (is (not (:hard? v)))
      (is (:escalate? v)))))

(deftest always-escalates-high-risk-trip-even-at-high-confidence
  (testing "high-risk trip acceptance always requires human sign-off"
    (let [st (fresh-store)
          v (governor/check req {} {:op :approve-high-risk-trip :effect :propose
                                    :driver-id "D-1" :confidence 0.99 :stake :low} st)]
      (is (not (:hard? v)))
      (is (:escalate? v)))))

(deftest escalates-low-confidence
  (let [st (fresh-store)
        v (governor/check req {} (assoc (trip 5 2 10) :confidence 0.3) st)]
    (is (not (:hard? v)))
    (is (:escalate? v))))
