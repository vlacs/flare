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

(def default-opts
  {:timeout 60
   :user-agent "org.vlacs/flare/http-kit"
   :method :post
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
      (assoc :body payload)
      (assoc :url url)))

#_
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

