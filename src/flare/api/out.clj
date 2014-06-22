(ns flare.api.out
  (:require [datomic.api :as d]
            [hatch]
            [flare.db]
            [flare.db.queries :as queries]
            [flare.db.rules :as rules]
            [flare.util :as util]
            [flare.event :as event]
            [org.httpkit.client :as http]
            [taoensso.timbre :as timbre]))

(defn default-outgoing-fn!
  [message]
  (timbre/debug "Outgoing-fn! message:" message))

(defn tx-entity!->eid
  [db-conn entity-type attrs]
  (let [prepped-entity (hatch/ensure-db-id
                         flare.db/partitions
                         entity-type
                         attrs)]
    (when-let
      [rval
       @(flare.db/tx-entity!
          db-conn
          entity-type
          prepped-entity)]
      (d/resolve-tempid
        (d/db db-conn)
        (:tempids rval)
        (:db/id prepped-entity)))))

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

(defn claim-notifications!
  [db-conn client-eid batch-entity]
  (when (first (d/q queries/pending-subscription-notifications
                    (d/db db-conn)
                    rules/defaults
                    client-eid))
    (when-let [tx-rval
               (d/transact db-conn [[:flare/grab-notifications
                                     rules/defaults
                                     queries/pending-subscription-notifications
                                     client-eid
                                     batch-entity]])]
      (when (> (count (:tempids @tx-rval)) 0)
        (d/resolve-tempid (d/db db-conn) (:tempids @tx-rval) (:db/id batch-entity))))))

(defn fetch-batched-notifications
  [db-conn batch-eid]
  (d/q queries/subscriber-notification-entities-by-batch
       (d/db db-conn) batch-eid))

(defn notification-watcher
  [db-conn outgoing-fn! client-eid single-run? thread-eid]
  (when (not (fn? outgoing-fn!))
    (let [ex (ex-info "outgoing-fn! must be a fn!" {:outgoing-fn! outgoing-fn!})]
      (timbre/fatal ex)
      (throw ex)))
  (when (not single-run?)
    (timbre/info "Flare notification watcher thread started."
           {:client-eid client-eid :thread-eid thread-eid}))
  (loop [continue? true]
    (if (not continue?)
      true
      (let [batch-entity (hatch/ensure-db-id
                           flare.db/partitions
                           :thread-batch
                           {:thread-batch/thread thread-eid
                            :thread-batch/uuid (d/squuid)})
            batch-eid (claim-notifications! db-conn client-eid batch-entity)]
        (if (nil? batch-eid)
          (Thread/sleep 1000)
          (do
            (timbre/debug "Notifications grabbed. Processing them."
                          {:thread-eid thread-eid
                           :batch-eid batch-eid})
            ;;; Grab them and queue them up.
            (doseq [notification (fetch-batched-notifications db-conn batch-eid)]
              (outgoing-fn! (first notification)))))
        (recur (and continue? (not single-run?)))))))

(defn make-notification-watcher-thread
  [db-conn outgoing-fn! client-eid]
  (let [thread-squuid (d/squuid)]
    (when-let [thread-eid (tx-entity!->eid db-conn :thread {:thread/uuid thread-squuid})]
      (timbre/info "New thread created." {:uuid thread-squuid
                                           :eid thread-eid})
      (Thread. (fn make-notification-watcher-thread- []
                 (.setName (Thread/currentThread) (str "Flare notification worker  " thread-eid))
                 (notification-watcher db-conn outgoing-fn! client-eid false thread-eid))))))

