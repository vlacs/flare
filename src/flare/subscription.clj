(ns flare.subscription
  (:require [datomic.api :as d]
            [flare.db]
            [flare.client :as client]
            [flare.event :as event]))

(def http-method-post :subscription.http-method/post)
(def http-method-put :subscription.http-method/put)
(def format-edn :subscription.format/edn)
(def format-json :subscription.format/json)


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
           event-type))))

(defn subscribe!
  [db-conn
   client-name
   event-type
   url-string
   http-method-keyword
   format-keyword]
  ;;; We can only subscribe to events and clients that are already registered.
  (let [client-eid (client/entity-id db-conn client-name)]
    (when (nil? client-eid)
      (throw
        (.Exception 
          (str "Cannot subscribe with a client that doesn't exist."))))
    (if
      (not
        (nil?
          @(flare.db/tx-entity!
             db-conn
             :subscription
             {:subscription/client client-eid
              :subscription/event.type event-type
              :subscription/url url-string
              :subscription/http-method http-method-keyword
              :subscription/format format-keyword})))
      :subscribed
      nil)))

(defn deactivate!
  "Deactivates a subscription so notifications aren't generates for it."
  [client-name event-type]
  :deactivated 
  )

(defn pause!
  [client-name event-type]
  :paused
  )
