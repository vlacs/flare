(ns flare.test-config
  (:require [datomic.api :as d]
            [datomic-schematode :as schematode]
            [stateful]
            [hatch]
            [flare.schema :as schema]
            [flare.client]
            [flare.event :as event]
            [flare.subscription :as sub]
            [flare.api.out]
            [taoensso.timbre :as timbre]
            [clojure.edn :as edn]))

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

(defn tx-test-subscriptions!
  [system-atom]
  (timbre/info "Making test subscriptions...")
  (let [dc (stateful/get-from system-atom [:db-conn])]
    (flare.subscription/subscribe!
      system-atom :moodle (flare.event/slam-event-type :flare :test-event-one)
      "http://fakeuri.com/api/v1/foo")
    (flare.subscription/subscribe!
      system-atom :showevidence (flare.event/slam-event-type :flare :test-event-one)
      "http://anotherfake.uri/a/v1/pi/bar"))
  system)

(defn tx-testing!
  [system-atom]
  (timbre/info "Assembling test database...")
  (stateful/transition! system-atom tx-test-schema!)
  (stateful/transition! system-atom tx-test-events!)
  (tx-test-subscriptions! system-atom)
  (stateful/transition! system-atom tx-test-event-attrs!))

(defn start-datomic! [system]
  (timbre/info "Starting Datomic peer...")
  (let [d-uri (get-in system [:config :datomic-uri])]
    (d/create-database d-uri)
    (assoc system :db-conn (d/connect d-uri))))

(defn stop-datomic! [system]
  (timbre/info "Stopping Datomic peer...")
  (d/delete-database (get-in system [:config :datomic-uri]))
  (dissoc system :db-conn))

(defn start!
  "Starts the current development system."
  [system-atom]
  (timbre/info "Bringing the test system up...")
  (stateful/load-config! system-atom [:config] "flare-config.edn")
  (stateful/transition! system-atom start-datomic!)
  (stateful/transition! system-atom init!))

(defn stop!
  "Shuts down and destroys the current development system."
  [system-atom]
  (timbre/info "Shutting down the test system...")
  (when (not (nil? (stateful/get-from system-atom [:db-conn])))
    (stateful/transition! system-atom stop-datomic!)))

(defn testing-fixture [system-atom f]
  (start! system-atom)
  (f)
  (stop! system-atom))

