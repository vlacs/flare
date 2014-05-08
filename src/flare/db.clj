(ns flare.db
  (:require [flare.schema :as schema]
            [datomic-schematode.core :as schematode]
            [hatch]))

(defn init-database!
  "An independent fn that initializes the state of flare before anything is
  started. This should only be called from flare/init!"
  [new-database]
  [(schematode/init-schematode-constraints! new-database)
   (schematode/load-schema! new-database schema/schema)])

(def partitions (merge {:event-type :db.user}
                       (hatch/schematode->partitions schema/schema)))
(def valid-attrs (merge {:event-type [:db.ident]}
                        (hatch/schematode->attrs schema/schema)))
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
