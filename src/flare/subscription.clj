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
  [system client-name event-type]
  (when-let [eid (get-entity-id (:db-conn system)
                                client-name
                                event-type)]
    (when-let [e (d/entity (:db-conn system) eid)]
      (into {} e))))

(defn subscribe!
  "This function changes the system to reflect the transformation fn's relation
  to the client and the event-type. This fn returns the updated system.

  The api-transformation-fn that takes two arguments, the first is the prepared
  http-kit options map for the call to be made. The second is the data that is
  to be sent to the third party via the http options. A 2 item vector with the
  transformed options followed by the transformed data."
  [system
   client-name
   event-type
   url-string
   api-transformation-fn]
  ;;; We can only subscribe to events and clients that are already registered.
  (let [db-conn (:db-conn system)]
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
            (assoc-in system [:flare :transformations
                              client-name event-type]
                      api-transformation-fn))
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


