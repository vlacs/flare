(ns flare.api.out)

(def default-opts
  {:timeout 60
   :user-agent "Flare API caller (http-kit 2.1.16)"
   :headers {:Accept "application/json"
             :Accept-Charset "utf-8"
             :Cache-Control "no-cache"
             :Connection "keep-alive"
             :X-Requested-With "http-kit (2.1.16)"}
   :keepalive 10000
   :insecure? false
   :follow-redirects false})

