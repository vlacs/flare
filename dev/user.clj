(ns user
  "Tools for interactive development with the REPL. This file should
  not be included in a production build of the application."
  (:require
    [clojure.java.io :as io]
    [clojure.java.javadoc :refer (javadoc)]
    [clojure.pprint :refer (pprint)]
    [clojure.reflect :refer (reflect)]
    [clojure.repl :refer (apropos dir doc find-doc pst source)]
    [clojure.set :as set]
    [clojure.string :as str]
    [clojure.test :as test]
    [clojure.tools.namespace.repl :refer (refresh refresh-all)]
    [datomic.api :as d]
    [datomic-schematode :as schematode]
    [clojure.edn :as edn]
    [stateful]
    [flare]
    [flare.schema :as schema]
    [flare.test-config :as ft-config]
    [hatch]
    #_[helmsman]))

;;; Local database connection to use in the REPL during development.
(def db nil)
(def system (stateful/init! nil))

(defn go
  ([]
   (go system))
  ([mutable-system]
   ;;; Start our testing enviornment.
   (stateful/init! mutable-system {})
   (ft-config/start! mutable-system)
   (stateful/transition! mutable-system (partial flare/init! true))
   (stateful/transition! mutable-system flare/start!)
   (ft-config/tx-testing! mutable-system)
   ;;; Bind the database connection so devs can use it.
   ;;; Do we really want this or something less var-ish? --jdoane
   (alter-var-root
     #'user/db
     (constantly
       (stateful/get-from mutable-system [:db-conn])))))

(defn reset
  "Stops the system, reloads modified source files, and restarts it."
  ([]
   (reset system))
  ([mutable-system]
   (when (not (nil? (stateful/get-state mutable-system)))
     (stateful/transition! mutable-system flare/stop!)
     (ft-config/stop! mutable-system))
   (stateful/destroy! mutable-system) 
   (refresh :after 'user/go)))

(defn touch-that
  "Execute the specified query on the current DB and return the
  results of touching each entity.

  The first binding must be to the entity.
  All other bindings are ignored."
  [query & data-sources]
  (let [db-conn (stateful/get-from system [:db-conn])
        db-val (d/db db-conn)]
    (map #(d/touch
            (d/entity
              db-val
              (first %)))
         (apply d/q query db-val data-sources))))

(defn ptouch-that
  "Example: (ptouch-that '[:find ?e :where [?e :user/username]])"
  [query & data-sources]
  (pprint (apply touch-that query data-sources)))

