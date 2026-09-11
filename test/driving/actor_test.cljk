(ns driving.actor-test
  (:require [clojure.test :refer [deftest is testing]]
            [driving.actor :as actor]
            [driving.store :as store]))

(defn- fresh-store []
  (let [st (store/mem-store)]
    (store/register-client! st {:client-id "client-1" :name "Kobo Driving Co"})
    (store/register-driver! st {:driver-id "D-1" :client-id "client-1"
                                :name "route-van-3"
                                :max-daily-duty-hours 10
                                :inspection-validity-days 30})
    st))

(deftest commits-an-in-duty-hours-valid-inspection-trip
  (let [st (fresh-store)
        graph (actor/build-graph {:store st})
        request {:client-id "client-1" :op :approve-trip :stake :low
                 :driver-id "D-1" :hours-worked-today 5 :trip-duration-hours 2
                 :days-since-inspection 10}
        result (actor/run-request! graph request {} "thread-1")]
    (is (= :done (:status result)))
    (is (some? (get-in result [:state :record])))
    (is (= 1 (count (store/records-of st "client-1"))))))

(deftest holds-an-over-duty-hours-trip
  (let [st (fresh-store)
        graph (actor/build-graph {:store st})
        request {:client-id "client-1" :op :approve-trip :stake :low
                 :driver-id "D-1" :hours-worked-today 9 :trip-duration-hours 5
                 :days-since-inspection 10}
        result (actor/run-request! graph request {} "thread-2")]
    (is (= :hold (:disposition (:state result))))
    (is (empty? (store/records-of st "client-1")))))

(deftest interrupts-then-approves-high-risk-trip-on-human-approval
  (let [st (fresh-store)
        graph (actor/build-graph {:store st})
        request {:client-id "client-1" :op :approve-high-risk-trip :stake :low
                 :driver-id "D-1"}
        interrupted (actor/run-request! graph request {} "thread-3")]
    (is (= :interrupted (:status interrupted)))
    (is (empty? (store/records-of st "client-1")))
    (let [resumed (actor/approve! graph "thread-3")]
      (is (= :done (:status resumed)))
      (is (= 1 (count (store/records-of st "client-1")))))))
