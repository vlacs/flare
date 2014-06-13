(ns flare
  (:require [datomic.api :as d]
            [flare.db]
            [flare.db.queries]
            [flare.db.rules]
            [flare.client]
            [flare.schema]
            [flare.event]
            [flare.api.out]
            [hatch]
            [taoensso.timbre :as timbre]
            ))

(def schema flare.schema/schema)
(def event flare.event/event)

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

(defn configure!
  "Brings flare up so we'll be ready for business."
  [system]
  ;;; Create the clients that are inside the system.
  (timbre/debug "Configuring Flare...")
  (let [db-conn (:db-conn system)]
    (doseq [client (get-in system [:attaches :endpoints])]
      (when (not (flare.client/registered? db-conn client))
        (timbre/debug "Found a new client... Adding it." client)
        (flare.client/add! (:db-conn system) client (str (d/squuid))))))
  system)

(defn start!
  [system]
  (assoc-in system [:flare :notification-threads]
    (into 
      {}
      (let [threads (get-in system [:config :flare :threads-per-client] 1)]
        (for [client (get-in system [:attaches :endpoints])]
          (let [client-eid (flare.client/get-entity-id (:db-conn system) client)]
            [client-eid
             (doall (for [t (repeatedly
                              threads
                              (partial
                                flare.api.out/make-notification-watcher-thread
                                (:db-conn system)
                                (get-in system [:flare :outgoing-fns client])
                                client-eid))]
                      (do
                        (.start t)
                        t)))]))))))

(defn stop!
  "Wraps up everything flare is working on so we can cleanly shutdown."
  [system]
  (doseq [t (get-in system [:flare :notification-threads])]
    (timbre/debug "Client watcher threads for: " (first t))
    (doseq [thr (second t)]
      (timbre/debug "Stopping thread.")
      (.stop thr)
      (timbre/debug "Thread stopped."))
    (timbre/debug "Client watchers stopped for: " (first t)))
  system)

