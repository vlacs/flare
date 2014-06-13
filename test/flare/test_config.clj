(ns flare.test-config
  (:require [datomic.api :as d]
            [datomic-schematode :as schematode]
            [flare.schema :as schema]
            [flare.client]
            [flare.event :as event]
            [flare.subscription :as sub]
            [flare.api.out]
            [taoensso.timbre :as timbre]))

(def system {:datomic-uri "datomic:mem://flare-test"
             :config {:flare
                      {:threads-per-client 1}}
             :attaches {:endpoints
                        [:moodle :showevidence]
                        :outgoing-fns
                        {:moodle flare.api.out/default-outgoing-fn!
                         :showevidence flare.api.out/default-outgoing-fn!}}})
(def datomic-uri (:datomic-uri system))
(def testing-auth-token "abc123")
(def testing-events [[:flare :ping]
                     [:flare :event-update]])
(def testing-url "http://some.fake.url.com/api/v1")
(def default-http-method :post)

(defn init! [system]
  [(schematode/init-schematode-constraints! (:db-conn system))
   (schematode/load-schema! (:db-conn system) schema/schema)])

(defn tx-testing-data!
  [system]
  (let [db-conn (get system :db-conn)]
    (timbre/debug "Starting to transact testing data.")
    (doseq [et testing-events]
      (apply (partial flare.event/register! db-conn) et)) 
    (doseq [c (get-in system [:attaches :endpoints])]
      (doseq [et testing-events]
        (flare.subscription/subscribe!
          db-conn c (apply event/slam-event-type et)
          testing-url flare.subscription/http-method-post
          flare.subscription/format-json)))
    (dotimes [n 5]
      (flare.api.out/make-ping-event! db-conn))
    (timbre/debug "Testing data has successfully been transacted."))
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
