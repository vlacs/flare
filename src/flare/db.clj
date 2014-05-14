(ns flare.db
  (:require 
    [datomic.api :as d]
    [flare.schema :as schema]
    [datomic-schematode.core :as schematode]
    [hatch]))

(defn init-database!
  "An independent fn that initializes the state of flare before anything is
  started. This should only be called from flare/init!"
  [new-database]
  [(schematode/init-schematode-constraints! new-database)
   (schematode/load-schema! new-database schema/schema)])

#_
(def partitions (merge {:event.type :db.part/user}
                       (hatch/schematode->partitions schema/schema)))

(def partitions (hatch/schematode->partitions schema/schema))
(def valid-attrs (merge-with
                  (fn [x y] (vec (concat x y)))
                   {:event.type [:db/ident]}
                   (hatch/schematode->attrs schema/schema)))
(def clean-entity (partial hatch/clean-entity partitions valid-attrs))
(def tx-entity! (partial hatch/tx-clean-entity! partitions valid-attrs))
(defn new-tempid
  [entity-type]
  (d/tempid (entity-type partitions)))
(defn enum-keyword [entity-type attr-name kw]
  (keyword (str (name entity-type) "." (name attr-name)) (name kw)))

;;; TODO: Move this to hatch?
(defn strip-nils
  "Datomic doesn't allow attributes to be nil, so there are instance where we
  want to strip them out before datomic gets a hold of it."
  [entity-map]
  (apply
    dissoc
    entity-map
    (for [[k v] entity-map :when (nil? v)] k))) 
