(ns flare.subscription
  (:require [datomic.api :as d]
            [flare.db]
            [flare.event :as event]))

(defn client-entity-id
  [db-conn client-name]
  (first
    (first
      (d/q '[:find ?e
             :in $ ?name
             :where [?e :client/name ?name]]
           (d/db db-conn)
           client-name))))

(defn client-registered?
  [db-conn client-name]
  (not (nil? (client-entity-id db-conn client-name))))

(defn add-client!
  [db-conn client-name auth-token]
  (if
    (not
      (nil?
        @(flare.db/tx-entity!
           db-conn
           :client
           {:client/name client-name
            :client/auth-token auth-token})))
    (client-entity-id client-name)
    (throw
      (.Exception
        (str "Adding flare client to the database failed.")))))

(defn revoke-client!
  []
  :removed
  )

(defn subscription-entity-id
  [db-conn client-name event-type]
  (first
    (first
      (d/q '[:find ?subscription
             :in $ ?client-name ?event-type
             :where
             [?client :client/name ?name]
             [?event :event/type ?event-type]
             [?subscription :subscription/event ?event]
             [?subscription :subscription/client ?client]]
           (d/db db-conn)
           client-name
           event-type)))
  )

(defn subscribe!
  [db-conn
   client-name
   event-type
   url-string
   http-method-keyword
   format-keyword]
  ;;; We can only subscribe to events and clients that are already registered.
  (let [client-eid (client-entity-id db-conn client-name)
        event-eid (event/event-entity-id db-conn event-type)]
    (when (nil? client-eid)
      (throw
        (.Exception 
          (str "Cannot subscribe with a client that doesn't exist."))))
    (when (nil? event-eid)
      (throw
        (.Exception
          (str "Cannot subscribe to an event that isn't registered."))))
    (if
      (not
        (nil?
          @(flare.db/tx-entity!
             db-conn
             :subscription
             {:subscription/client client-eid
              :subscription/event event-eid
              :subscription/url url-string
              :subscription/http-method http-method-keyword
              :subscription/format format-keyword})))
      )
    )
  
  :subscribed
  )

(defn unsubscribe!
  ([subscription-id]
   :a-specific-subscription)
  ([client event]
   :a-deterministic-subscription))
