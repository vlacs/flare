(ns flare.test-config
  (:require [datomic.api :as d]
            [datomic-schematode :as schematode]
            [hatch]
            [flare.schema :as schema]
            [flare.client]
            [flare.event :as event]
            [flare.subscription :as sub]
            [flare.api.out]
            [taoensso.timbre :as timbre]
            [clojure.edn :as edn]))

(def system {:attaches
             {:endpoints
              [:moodle :showevidence]}})

;;; This is our testing database schema that we'll do event testing on.
(def test-schema
  [{:namespace :test-entity-one
    :attrs [[:some-string :string]
            [:some-int :long]
            [:some-ref :ref]]}
   {:namespace :test-entity-two
    :attrs [[:another-string :string
             :some-float :float]]}])

(def test-partitions (hatch/schematode->partitions test-schema))
(def test-attrs (hatch/schematode->attrs test-schema))
(def tx-test-entity! (partial hatch/tx-clean-entity!
                             test-partitions
                             test-attrs))

(defn init! [system]
  (timbre/info "Loading schematode constraints and flare schema...")
  (schematode/init-schematode-constraints! (:db-conn system))
  (schematode/load-schema! (:db-conn system) schema/schema)
  system)

(defn tx-test-schema!
  [system]
  (timbre/info "Loading test schema...")
  (let [db-conn (:db-conn system)]
    (schematode/load-schema! db-conn test-schema))
  system)

(defn tx-test-events!
  [system]
  (timbre/info "Loading test events...")
  (let [db-conn (:db-conn system)]
    (doseq [evt [[:flare :test-event-one
                  [:test-entity-one/some-string]]
                 [:flare :test-event-two
                  [:test-entity-one/some-int
                   :test-entity-one/some-ref]]]]
      (apply (partial flare.event/register! db-conn) evt)))
  system)

(defn tx-test-event-attrs!
  [system]
  (timbre/info "Loading test data...")
  (let [db-conn (:db-conn system)]
    (tx-test-entity! db-conn :test-entity-one
                     (hatch/slam-all
                       {:some-string "abc123"
                        :some-int 1
                        :some-ref :test-entity-one/some-int}
                       :test-entity-one)))
  system)

(defn test-transformation
  [request-opts data]
  [request-opts data])

(defn tx-test-subscriptions!
  [system]
  (timbre/info "Making test subscriptions...")
  (-> system
      (flare.subscription/subscribe!
        :moodle (flare.event/slam-event-type :flare :test-event-one)
        "http://fakeuri.com/api/v1/foo"
        test-transformation)
      (flare.subscription/subscribe!
        :showevidence (flare.event/slam-event-type :flare :test-event-one)
        "http://anotherfake.uri/a/v1/pi/bar"
        test-transformation)))

(defn tx-testing!
  [system]
  (timbre/info "Assembling test database...")
  (-> system
      tx-test-schema!
      tx-test-events!
      tx-test-subscriptions!
      tx-test-event-attrs!))

(defn start-datomic! [system]
  (timbre/info "Starting Datomic peer...")
  (d/create-database (:datomic-uri system))
  (assoc system :db-conn
         (d/connect (get-in system [:datomic-uri]))))

(defn stop-datomic! [system]
  (timbre/info "Stopping Datomic peer...")
  (dissoc system :db-conn)
  (d/delete-database (:datomic-uri system))
  system)

(defn start!
  "Starts the current development system."
  []
  (timbre/info "Bringing the test system up...")
  (timbre/info "Reading flare-config.edn...")
  (let [conf (edn/read-string (slurp "flare-config.edn"))]
    (alter-var-root #'system merge conf)
    (alter-var-root #'system start-datomic!))
  (init! system))

(defn stop!
  "Shuts down and destroys the current development system."
  []
  (timbre/info "Shutting down the test system...")
  (alter-var-root #'system
                  (fn [s] (when s (stop-datomic! s)))))

(defn testing-fixture [f]
  (start!)
  (f)
  (stop!))

