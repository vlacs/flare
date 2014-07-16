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
   [flare]
   [flare.schema :as schema]
   [flare.test-config :as ft-config]
   [hatch]
   #_[helmsman]))

;;; Local database connection to use in the REPL during development.
(def db nil)

(defn go []
  ;;; Start our testing enviornment.
  (ft-config/start!)
  ;;; Bind the database connection so devs can use it.
  (alter-var-root #'user/db (constantly (:db-conn ft-config/system))) 

  ;;; Initialize flare including loading the schema.
  (alter-var-root 
    #'ft-config/system 
    (constantly
      (-> (flare/init! true ft-config/system)
          flare/configure!
          flare/start!
          ft-config/tx-testing!))))

(defn reset
  "Stops the system, reloads modified source files, and restarts it."
  []
  (when (not (nil? (:db-conn ft-config/system)))
    ;;; Wrap up what we're doing.
    (flare/stop! ft-config/system)
    ;;; Close down our env.
    (ft-config/stop!))
  (refresh :after 'user/go))

(defn touch-that
  "Execute the specified query on the current DB and return the
   results of touching each entity.

   The first binding must be to the entity.
   All other bindings are ignored."
  [query & data-sources]
  (map #(d/touch
         (d/entity
          (d/db (:db-conn ft-config/system))
          (first %)))
       (apply d/q query (d/db (:db-conn ft-config/system)) data-sources)))

(defn ptouch-that
  "Example: (ptouch-that '[:find ?e :where [?e :user/username]])"
  [query & data-sources]
  (pprint (apply touch-that query data-sources)))
 
