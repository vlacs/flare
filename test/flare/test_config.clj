(ns flare.test-config
  (:require [datomic.api :as d]
            [datomic-schematode :as schematode]
            [hatch]
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

;;; This is our testing database schema that we'll do event testing on.
(def test-schema
  [{:namespace :test-entity-one
    :attrs [[:some-string :string]
            [:some-int :bigint]
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
  [(schematode/init-schematode-constraints! (:db-conn system))
   (schematode/load-schema! (:db-conn system) schema/schema)])

(defn tx-test-schema!
  [db-conn]
  (timbre/info "Loading test schema...")
  (schematode/load-schema! db-conn test-schema)
  db-conn)

(defn tx-test-events!
  [db-conn]
  (timbre/info "Loading test events...")
  (doseq [evt [[:flare :test-event-one
                [:test-entity-one/some-string]]
               [:flare :test-event-two
                [:test-entity-one/some-int
                 :test-entity-one/some-ref]]]]
    (apply (partial flare.event/register! db-conn) evt))
  db-conn)

(defn tx-test-event-attrs!
  [db-conn]
  (timbre/info "Loading test data...")
  (tx-test-entity! db-conn :test-entity-one
                   (hatch/slam-all
                     {:some-string "abc123"
                      :some-int 1
                      :some-ref :test-entity-one/some-int}
                     :test-entity-one))
  db-conn)

(defn tx-testing!
  [db-conn]
  (timbre/info "Assembling test database...")
  (-> db-conn
      tx-test-schema!
      tx-test-events!
      tx-test-event-attrs!))

(defn start-datomic! [system]
  (timbre/info "Starting Datomic peer...")
  (d/create-database (:datomic-uri system))
  (assoc system :db-conn
         (d/connect (:datomic-uri system))))

(defn stop-datomic! [system]
  (timbre/info "Stopping Datomic peer...")
  (dissoc system :db-conn)
  (d/delete-database (:datomic-uri system))
  system)

(defn start!
  "Starts the current development system."
  []
  (timbre/info "Bringing the test system up...")
  (alter-var-root #'system start-datomic!)
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

