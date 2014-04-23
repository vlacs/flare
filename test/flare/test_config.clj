(ns flare.test-config
  (:require [datomic.api :as d]
            [datomic-schematode.core :as schematode]
            [flare.schema :as schema]))

(def system {:datomic-uri "datomic:mem://flare-test"})
(def datomic-uri (:datomic-uri system))

(defn init! [system]
  [(schematode/init-schematode-constraints! (:db-conn system))
   (schematode/load-schema! (:db-conn system) schema/schema)])

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
