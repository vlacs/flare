(ns flare
  (:require [flare.schema :as schema]
            [datomic.api :as d]
            [flare.db]
            [flare.db.queries]
            [flare.db.rules]
            [flare.event]
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

(def event flare.event/event)

(defn tx-notify!
  "Wrapper for hatch/tx! transacts and asyncronously adds the event to the
  queue for processing. The first item should always be the event entity to
  be transacted."
  [db-conn txs]
  (let [event-tempid (:db/id (first txs))
        dr @(hatch/tx! db-conn txs)
        db (d/db db-conn)]
    (d/q flare.db.queries/subscriptions-notifications
         db flare.db.rules/defaults
         (d/resolve-tempid db (:tempids dr) event-tempid))))
