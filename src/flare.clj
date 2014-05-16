(ns flare
  (:require [flare.schema :as schema]
            [flare.db]
            [hatch]))

(def schema flare.schema/schema)

(defn init!
  "Starts up flare for the first time. By default, we don't load up the schema.
  this is because Galleon will do it for us, but in development we will manually
  specify that we want to load it at this point."
  ([system]
   (init! false system))
  ([init-database? system]
   (when init-database?
     (flare.db/init-database! (:db-conn system)))
   system))

(defn start!
  "Brings flare up so we'll be ready for business."
  [system]
  ;;; Start up notification processing.
  system)

(defn stop!
  "Wraps up everything flare is working on so we can cleanly shutdown."
  [system]
  ;;; Shutdown notification processing
  system)

;;; This is old, we're going to update this soon. :)
(comment
  (flare.state/tx-entity! (:db-conn ft-config/system)
                          :subscription
                          {:subscription/uri "http://moc.com/"
                           :subscription/method :subscription.method/put
                           :subscription/event-type :burnt-toast})
  (ptouch-that '[:find ?e :where [?e :subscription/uri]])
  )


