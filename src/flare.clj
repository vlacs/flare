(ns flare
  (:require [flare.schema :as schema]
            [flare.state :as state]
            [hatch]))

(defn init!
  "Starts up flare for the first time."
  [system]
  (flare.state/init-database! (:db-conn system))
  :initialized)

(defn start!
  "Brings flare up so we'll be ready for business."
  [system]
  (state/set-db-var! (:db-conn system))
  :started)

(defn stop!
  "Wraps up everything flare is working on so we can cleanly shutdown."
  [system]
  (state/detach-db-var!)
  :stopped)

;;; This is old, we're going to update this soon. :)
(comment
  (flare.state/tx-entity! (:db-conn ft-config/system)
                          :subscription
                          {:subscription/uri "http://moc.com/"
                           :subscription/method :subscription.method/put
                           :subscription/event-type :burnt-toast})
  (ptouch-that '[:find ?e :where [?e :subscription/uri]])
  )


