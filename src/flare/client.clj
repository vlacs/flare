(ns flare.client
  (:require [datomic.api :as d]
            [flare.db]
            [flare.db.queries :as queries]
            [flare.db.rules :as rules]
            [hatch]))

(defn entity-id
  "Get the entity id of a client."
  [db-conn client-name]
  (ffirst
    (d/q queries/client-entity-id
         (d/db db-conn)
         client-name)))

(defn prep-new
  ([client-name auth-token]
   (prep-new client-name auth-token nil nil))
  ([client-name auth-token paused? inactive?]
   (hatch/slam-all
     {:name client-name
      :auth-token auth-token
      :paused? paused?
      :inactive? inactive?}
     :client)))

(defn prep-update
  [db-conn client-name attributes]
  (merge
    {:db/id (entity-id db-conn client-name)}
    (hatch/slam-all
      attributes
      :client)))

(defn upsert!
  [db-conn attributes]
  (flare.db/tx-entity!
    db-conn
    :client
    (flare.db/strip-nils attributes)))

(defn upserted?
  "Checks to see if an upsert! succeeded."
  [db-promise]
  (not (nil? @db-promise)))

(defn registered?
  "Is a client with a particular name registered?"
  [db-conn client-name]
  (not (nil? (entity-id db-conn client-name))))

(defn add!
  [db-conn client-name auth-token]
  (when
    (upserted?
      (upsert!
        db-conn
        (prep-new client-name auth-token)))
    :added))

(defn deactivate!
  "Stops flare from making notifications for a particular client."
  [db-conn client-name]
  (when
    (upserted?
      (upsert!
        db-conn
        (prep-update db-conn client-name {:inactive? true}))))
  :deactivated)

(defn activate!
  "Allows flare to start making notifications for a particular client."
  [db-conn client-name]
  (when
    (upserted?
      (upsert!
        db-conn
        (prep-update db-conn client-name {:inactive? false})))
    :activated))

(defn pause!
  "Stops processing notifications for a particular client."
  [db-conn client-name]
  (when
    (upserted?
      (upsert!
        db-conn
        (prep-update db-conn client-name {:paused? true})))
    :paused))

(defn resume!
  "Allows processing of this client's notifications."
  [db-conn client-name]
  (when
    (upserted?
      (upsert!
        db-conn
        (prep-update db-conn client-name {:paused? false})))
    :resumed))

