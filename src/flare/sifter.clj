(ns flare.sifter
  (:require 
    [flare.db]
    [datomic.api :as d]))

(def sifting-constant ::master-sifter)

(defn triggering-attr-eids
  [db-conn]
  nil
  )

(defn set-last-sift!
  [db-conn]
  (let [inst (java.util.Date.)]
    (when
      (flare.db/tx-entity! db-conn
                           :sift-singleton
                           {:db/ident sifting-constant
                            :sift-singleton/value inst})
      inst)))

(defn last-sift-at
  [db-conn]
  (when-let [ls (d/entity (d/db db-conn) sifting-constant)]
    (:sift-singleton/value (into {} ls))))

(defn datomic-tx-list
  [db-conn since-t]
  (d/log (d/since (d/db db-conn))))

(defn reduced-db
  [db-conn last-sift]
  (d/since (d/db db-conn) last-sift))
