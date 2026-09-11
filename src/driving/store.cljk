(ns driving.store
  "SSoT for the ISCO-08 8322 independent car & van driving practice
  actor (itonami actor pattern, ADR-2607011000 / CLAUDE.md Actors
  section; README's 'Robotics premise' — a vehicle-telemetry robot
  performs pre-trip inspection checks and cargo/passenger-area
  sensing under this advisor/governor pair, which never dispatches
  hardware itself). Modeled on cloud-itonami-isco-4311's
  bookkeeping.store.

  Domain:

    client — a registered organization (:client-id, :name)
    driver — a registered driver {:driver-id :client-id :name
             :max-daily-duty-hours number
             :inspection-validity-days number}.
             `:max-daily-duty-hours` is the registered ceiling a
             proposed trip's cumulative duty hours (already worked
             plus the trip) must not exceed — duty hours are
             cumulative arithmetic, not driver discretion;
             `:inspection-validity-days` is the registered window a
             proposed trip's days-since-last-inspection must not
             exceed — a trip on an expired inspection is not a
             documented vehicle.
    record — a committed operating record (approved trip) — written
             ONLY via commit-record!.
    ledger — append-only audit trail, commit or hold."
  )

(defprotocol Store
  (client [s client-id])
  (driver [s driver-id])
  (records-of [s client-id])
  (ledger [s])
  (register-client! [s client])
  (register-driver! [s d])
  (commit-record! [s record])
  (append-ledger! [s fact]))

(defrecord MemStore [a]
  Store
  (client [_ client-id] (get-in @a [:clients client-id]))
  (driver [_ driver-id] (get-in @a [:drivers driver-id]))
  (records-of [_ client-id] (filter #(= client-id (:client-id %)) (:records @a)))
  (ledger [_] (:ledger @a))
  (register-client! [s client]
    (swap! a assoc-in [:clients (:client-id client)] client) s)
  (register-driver! [s d]
    (swap! a assoc-in [:drivers (:driver-id d)] d) s)
  (commit-record! [s record]
    (swap! a update :records (fnil conj []) record) s)
  (append-ledger! [s fact]
    (swap! a update :ledger (fnil conj []) fact) s))

(defn mem-store
  ([] (mem-store {}))
  ([seed] (->MemStore (atom (merge {:clients {} :drivers {} :records [] :ledger []}
                                   seed)))))
