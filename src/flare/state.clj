(ns flare.state
  (:require [flare.schema :as schema]
            [datomic-schematode.core :as schematode]
            [hatch]))

(defn init-database!
  "An independent fn that initializes the state of flare before anything is
  started. This should only be called from flare/init!"
  [new-database]
  [(schematode/init-schematode-constraints! new-database)
   (schematode/load-schema! new-database schema/schema)])
 
;;; We want a local reference to the database. This will get set by either the
;;; dev/user.clj file or by Galleon's start-fn!.
(def db nil)

(defn set-db-var!
  "Set the database that we're using for flare."
  [database]
  (alter-var-root #'db (constantly database)))

(defn detach-db-var! [] (alter-var-root #'db (constantly nil)))

(def partitions (hatch/schematode->partitions schema/schema))
(def valid-attrs (hatch/schematode->attrs schema/schema))
(def tx-entity! (partial hatch/tx-clean-entity! partitions valid-attrs))

;;; TODO: Move this to hatch?
(defn strip-nils
  "Datomic doesn't allow attributes to be nil, so there are instance where we
  want to strip them out before datomic gets a hold of it."
  [entity-map]
  (apply
    dissoc
    entity-map
    (for [[k v] entity-map :when (nil? v)] k))) 
