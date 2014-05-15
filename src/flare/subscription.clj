(ns flare.subscription
  (:require [datomic.api :as d]
            [flare.util]
            [flare.db]
            [flare.db.rules :as rules]
            [flare.db.queries :as queries]
            [flare.client :as client]
            [flare.event :as event]))

(def http-method-post :subscription.http-method/post)
(def http-method-put :subscription.http-method/put)
(def format-edn :subscription.format/edn)
(def format-json :subscription.format/json)

(defn subscription-entity-id
  [db-conn client-name event-type]
  (ffirst
    (d/q queries/subscription-entity-id
         (d/db db-conn)
         client-name
         event-type)))

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
  "Deactivates a subscription so notifications aren't generated for it."
  [client-name event-type]
  :deactivated 
  )

(defn pause!
  ([db-conn entity-id]
   (flare.db/tx-entity!
     :subscription
     {:db/id entity-id
      :subscription/paused? true}))
  ([db-conn client-name event-type]
   (pause! db-conn
           (flare.util/required
             (partial
               pause!
               db-conn
               (subscription-entity-id
                 db-conn
                 client-name
                 event-type))
             "Subscription specified does not exist."
             {:client-name client-name
              :event-type event-type}))))

