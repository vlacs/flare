(ns flare.api.out
  (:require 
            [org.httpkit.client :as http]
            [taoensso.timbre :as timbre]))

(def default-opts
  {:timeout 60
   :user-agent "org.vlacs/flare/http-kit"
   :headers {:Accept-Charset "utf-8"
             :Cache-Control "no-cache"
             :Connection "keep-alive"
             :X-Requested-With "clojure/http-kit"}
   :keepalive 10000
   :insecure? false
   :follow-redirects false})

(defn call-api!
  [request-map]
  (timbre/info (str "API call (" (name (:method request-map))
                    ") to: " (:url request-map)))
  (http/request request-map))

