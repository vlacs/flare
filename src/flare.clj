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
  (timbre/debug "Endpoints?" (get-in system [:attaches :endpoints]))
  (let [db-conn (:db-conn system)]
    (doseq [client (get-in system [:attaches :endpoints])]
      (when (not (flare.client/registered? db-conn client))
        (timbre/debug "Found a new client... Adding it." client)
        (flare.client/register! (:db-conn system) client (str (d/squuid))))))
  (assoc-in system [:flare :transformations]
            (into {}
                  (map (fn [i] [i {}]) (get-in system [:attaches :endpoints])))))

(defn start!
  [system]
  system)

(defn stop!
  "Wraps up everything flare is working on so we can cleanly shutdown."
  [system]
  system)

