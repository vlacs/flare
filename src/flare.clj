(ns flare
  (:require [flare.schema :as schema]
            [hatch]))

(def partitions (hatch/schematode->partitions schema/schema))
(def valid-attrs (hatch/schematode->attrs schema/schema))

(def tx-entity! (partial hatch/tx-clean-entity! partitions valid-attrs))


(comment
  (flare/tx-entity! (:db-conn ft-config/system)
                    :subscription
                    {:subscription/uri "http://moc.com/"
                     :subscription/method :subscription.method/put
                     :subscription/event-type :burnt-toast})
  (ptouch-that '[:find ?e :where [?e :subscription/uri]])
  )


