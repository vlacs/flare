(ns flare.client
  (:require [datomic.api :as d]
            [flare.db :refer [new-upserter-fn upserted?]]
            [flare.db.queries :as queries]
            [flare.db.rules :as rules]
            [hatch]))

(defn get-entity-id
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

(def upsert! (flare.db/new-upserter-fn :client))
(def set-attr! (flare.db/new-set-attr-fn upsert!))

(defn registered?
  "Is a client with a particular name registered?"
  [db-conn client-name]
  (not (nil? (get-entity-id db-conn client-name))))

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
   (when-let [entity-id (get-entity-id db-conn client-name)]
     (when
       (set-attr! db-conn entity-id :client/inactive? true)
       :deactivated)))

(defn activate!
  "Allows flare to start making notifications for a particular client."
  [db-conn client-name]
  (when-let [entity-id (get-entity-id db-conn client-name)]
    (when
      (set-attr! db-conn entity-id :client/inactive? false)
      :activated)))

(defn pause!
  [db-conn client-name]
  (when-let [entity-id (get-entity-id db-conn client-name)]
    (when
      (set-attr! db-conn entity-id :client/paused? true)
      :paused)))

(defn resume!
  [db-conn client-name]
  (when-let [entity-id (get-entity-id db-conn client-name)]
    (when
      (set-attr! db-conn entity-id :client/paused? false)
      :resumed)))

