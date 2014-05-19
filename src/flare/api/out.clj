(ns flare.api.out
  (:require [clojure.edn :as edn]
            [clojure.data.json :as json]
            [org.httpkit.client :as http]))

(def method-translation
  {:subscription.http-method/put :put
   :subscription.http-method/post :post})
 
(def default-opts
  {:timeout 60
   :user-agent "Flare API caller (clj/http-kit 2.1.16)"
   :headers {:Accept "application/json"
             :Accept-Charset "utf-8"
             :Cache-Control "no-cache"
             :Connection "keep-alive"
             :X-Requested-With "http-kit (2.1.16)"}
   :keepalive 10000
   :insecure? false
   :follow-redirects false})

(defn edn->json
  "Converts an EDN payload into a json object."
  [convertable-string]
  (json/write-str (edn/read-string convertable-string)))

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
    :subscription.format/json (edn->json payload)
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

