(ns flare.subscription
  (:require [datomic.api :as d]
            [flare.util]
            [flare.db :refer [upserted? new-upserter-fn]]
            [flare.db.rules :as rules]
            [flare.db.queries :as queries]
            [flare.client :as client]
            [flare.event :as event]
            [taoensso.timbre :as timbre]))


(def upsert! (flare.db/new-upserter-fn :subscription))
(def set-attr! (flare.db/new-set-attr-fn upsert!))

(defn prep-new
  [client-eid
   event-type
   url-string]
  (hatch/slam-all
    {:client client-eid
     :event.type event-type
     :url url-string
     } :subscription))

(defn get-entity-id
  [db-conn client-name event-type]
  (ffirst
    (d/q queries/subscription-entity-id
         (d/db db-conn)
         rules/defaults
         client-name
         event-type)))

(defn get-subscription
  [db-conn client-name event-type]
  (when-let [eid (get-entity-id db-conn 
                                client-name
                                event-type)]
    (when-let [e (d/entity (d/db db-conn) eid)]
      (into {} e))))

(defn subscribed?
  [db-conn client-name event-type]
  (not (nil? (get-entity-id db-conn client-name event-type))))

(defn subscribe!
  "Subscribes to a particular kind of event for a particular client."
  [db-conn
   client-name
   event-type
   url-string]
  ;;; We can only subscribe to events and clients that are already registered.
  (if (subscribed? db-conn client-name event-type)
    :exists
    (if-let [client-eid (client/get-entity-id db-conn client-name)]
      (let [new-sub-entity (prep-new client-eid
                                     event-type
                                     url-string)]
        (if
          (upserted?
            (upsert!
              db-conn
              new-sub-entity))
          (do
            (timbre/info "Subscription added." client-name event-type)
            :added)
          (timbre/error "Failed to add the subscription to Datomic."
                        new-sub-entity)))
      (timbre/error "Aborting subscription creation. No such client." client-name)
      )))

(defn alter-sub-attr!
  [db-conn client-name event-type attr value]
  (if-let [entity-id (get-entity-id db-conn client-name event-type)]
    (do
      (timbre/debug "Subscription attribute asserted (k v): " attr value)
      (set-attr! db-conn entity-id attr value))
    (timbre/error "Unable to assert subscription attribute. Subscription does not exist."
                  client-name event-type)))

(defn activate!
  [db-conn client-name event-type]
  (alter-sub-attr! db-conn client-name event-type :subscription/inactive? false))

(defn deactivate!
  "Deactivates a subscription so notifications aren't generated for it."
  [db-conn client-name event-type]
  (alter-sub-attr! db-conn client-name event-type :subscription/inactive? true))

(defn pause!
  [db-conn client-name event-type]
  (alter-sub-attr! db-conn client-name event-type :subscription/paused? true))

(defn resume!
  [db-conn client-name event-type]
  (alter-sub-attr! db-conn client-name event-type :subscription/paused? false))


