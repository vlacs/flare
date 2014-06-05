(ns flare.api.out
  (:require [datomic.api :as d]
            [flare.db]
            [flare.db.queries :as queries]
            [flare.db.rules :as rules]
            [flare.util :as util]
            [org.httpkit.client :as http]
            [taoensso.timbre :as timbre]))

(defn default-outgoing-fn!
  [message]
  (timbre/debug (str "Outgoing-fn! message:\n"
                     (util/pprint->str message))))

(def method-translation
  {:subscription.http-method/put :put
   :subscription.http-method/post :post})
 
(def default-opts
  {:timeout 60
   :user-agent "org.vlacs/flare/http-kit"
   :headers {:Accept "application/json"
             :Accept-Charset "utf-8"
             :Cache-Control "no-cache"
             :Connection "keep-alive"
             :X-Requested-With "clojure/http-kit"}
   :keepalive 10000
   :insecure? false
   :follow-redirects false})


(defn prep-options
  [method url auth-token payload]
  (-> default-opts
      (assoc-in [:headers :Authorization] auth-token)
      (assoc :method (method method-translation))
      (assoc :body payload)
      (assoc :url url)))

(defn prep-payload
  [payload data-format]
  (case data-format
    :subscription.format/json (util/edn->json payload)
    payload))

(defn call-api
  "Remote API to call to inform about an event.
  method is a subscription http-method.
  url is a good ol web address to the api you're calling.
  auth-token is our super awesome security validation token.
  outgoing-format is a subscription.format enum value.
  payload is what you're telling the remote system."
  [method url auth-token outgoing-format payload]
  (http/request
    (prep-options
      (method method-translation)
      url
      auth-token
      (prep-payload outgoing-format payload))))

(defn serialize-outgoing
  [notification-eid payload
   version url auth-token
   http-method pl-format]
  (util/->json
    {:notification-eid notification-eid
     :payload payload
     :version version
     :url url
     :auth-token auth-token
     :http-method http-method
     :format pl-format}))

(defn unserialize-outgoing
  [data-str]
  (util/json-> data-str)) 

(defn make-general-processor
  [watcher-fn]
  (Thread. watcher-fn))

(defn claim-notifications
  [db-conn thread-uuid batch-uuid]
  (d/transact db-conn [[:flare/grab-notifications
                        rules/defaults
                        queries/pending-subscription-notifications
                        thread-uuid
                        batch-uuid]]))

(defn fetch-batched-notifications
  [db-conn batch-uuid]
  (d/q queries/notifications-by-batch-uuid
       (d/db db-conn) rules/defaults batch-uuid))

(defn notification-watcher
  [db-conn outgoing-fn! single-run? thread-uuid]
  (loop [continue? true]
    (if (not continue?)
      true
      (let [batch-uuid (d/squuid)
            tx-val (claim-notifications db-conn thread-uuid batch-uuid)]
        (when (nil? tx-val)
          (throw (ex-info "db tx fn :flare/grab-notifications returned nil."
                          {:thread-uuid thread-uuid
                           :batch-uuid batch-uuid})))
        (when (> (count (:tempids @tx-val)) 0)
          ;;; Grab them and queue them up.
          (doseq [notification (fetch-batched-notifications db-conn batch-uuid)]
            (outgoing-fn! notification)))
        (recur (and continue? (not single-run?)))))))

(defn make-notification-watcher-thread
  [db-conn outgoing-fn!]
  (let [thread-squuid (d/squuid)]
    (when (flare.db/tx-entity! db-conn :thread {:thread/uuid thread-squuid})
      (Thread. (partial notification-watcher
                        db-conn outgoing-fn!
                        false thread-squuid)))))
 
