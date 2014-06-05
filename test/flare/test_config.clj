(ns flare.test-config
  (:require [datomic.api :as d]
            [datomic-schematode :as schematode]
            [flare.schema :as schema]
            [flare.client]
            [flare.event]
            [flare.subscription]))

(def system {:datomic-uri "datomic:mem://flare-test"})
(def datomic-uri (:datomic-uri system))

(defn init! [system]
  [(schematode/init-schematode-constraints! (:db-conn system))
   (schematode/load-schema! (:db-conn system) schema/schema)])

(defn tx-testing-data!
  [db-conn]
  (flare.client/add! db-conn "ShowEvidence" "abc123")
  (flare.client/add! db-conn "VLACS" "123abc")
  (flare.event/register! db-conn :flare :some-event)
  (flare.event/register! db-conn :flare :ping)
  (flare.event/register! db-conn :flare :an-update)
  (flare.subscription/subscribe!
    db-conn "VLACS" :event.type/flare.some-event
    "http://foo.bar/api"
    flare.subscription/http-method-post
    flare.subscription/format-json)
  (flare.subscription/subscribe!
    db-conn "ShowEvidence" :event.type/flare.some-event
    "http://showevience.bar/api-v1/resty"
    flare.subscription/http-method-put
    flare.subscription/format-edn)
  :done)

(defn start-datomic! [system]
  (d/create-database datomic-uri)
  (assoc system :db-conn
         (d/connect datomic-uri)))

(defn stop-datomic! [system]
  (dissoc system :db-conn)
  (d/delete-database datomic-uri)
  system)

(defn start!
  "Starts the current development system."
  []
  (alter-var-root #'system start-datomic!)
  (init! system))

(defn stop!
  "Shuts down and destroys the current development system."
  []
  (alter-var-root #'system
                  (fn [s] (when s (stop-datomic! s)))))

(defn testing-fixture [f]
  (start!)
  (f)
  (stop!))

(comment
  [wrap-params]
  [wrap-trace :header :ui])
