(ns flare.client
  (:require [datomic.api :as d]
            [flare.db]
            [hatch]))

(defn entity-id
  "Get the entity id of a client."
  [db-conn client-name]
  (first
    (first
      (d/q '[:find ?e
             :in $ ?name
             :where [?e :client/name ?name]]
           (d/db db-conn)
           client-name))))

(defn upsert!
  [db-conn attributes]
  (flare.db/tx-entity!
    db-conn
    :client
    (hatch/slam)
    attributes))

(defn registered?
  "Is a client with a particular name registered?"
  [db-conn client-name]
  (not (nil? (entity-id db-conn client-name))))

(defn add!
  [db-conn client-name auth-token]
  (if
    (not
      (nil?
        @(upsert!
           {:client/name client-name
            :client/auth-token auth-token})))
    :client-added
    (throw
      (.Exception
        (str "Adding flare client to the database failed.")))))

(defn deactivate!
  "Stops flare from making notifications for a particular client."
  [db-conn client-name]
  :deactivated
  )

(defn activate!
  "Allows flare to start making notifications for a particular client."
  [db-conn client-name]
  :activated
  )

(defn pause!
  "Stops processing notifications for a particular client."
  [db-conn client-name]
  :paused
  )
 
(defn resume!
  "Allows processing of this client's notifications."
  [db-conn client-name]
  :resumed
  )
