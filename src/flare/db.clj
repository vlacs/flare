(ns flare.db
  (:require 
    [datomic.api :as d]
    [flare.schema :as schema]
    [datomic-schematode :as schematode]
    [hatch]))

(defn init-database!
  "An independent fn that initializes the state of flare before anything is
  started. This should only be called from flare/init!"
  [new-database]
  [(schematode/init-schematode-constraints! new-database)
   (schematode/load-schema! new-database schema/schema)])

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

(defn upserted?
  "Checks to see if an upsert! succeeded.
  TODO: Ensure the reliability of this fn."
  [db-promise]
  (not (nil? @db-promise)))

(defn map-result-keys
  "Uses a query map to determine the keys for values in the result set for
  the given query. It assumes that find only has ?variables as we chop off
  the first char of every symbol after we turn it into a string."
  [query-map results]
  (set
    (map
      (partial
        zipmap 
        (map
          (fn transient-result-key-mapper [i]
            (keyword (apply str (rest (str i)))))
          (:find query-map)))
      results)))

(defn qmap
  "This is a wrapper for datomic.api/q by using the datalog names in a query
  MAP to return a set of hash maps that's compatible with clojure.set."
  [query & args]
  (map-result-keys
    query
    (apply (partial d/q query) args)))

(defn new-upserter-fn
  "Higher order function that returns a fn that takes in a db connection
  and the attributes for the pre-defined entity type. It also strips out nils."
  [entity-type]
  (fn upserter
    [db-conn attrs]
    (tx-entity!
      db-conn
      entity-type
      (strip-nils attrs))))

(defn new-set-attr-fn
  "Higher order function that returns a fn that takes in a db conn
  , entity id, attribute keyword, and value and transacts it. The return
  value will be a boolean that represents the success of the transaction."
  [upserter-fn!]
  (fn set-attr
    [db-conn entity-id attr value]
    (upserted?
      (upserter-fn!
        db-conn
        {:db/id entity-id
         attr value}))))

