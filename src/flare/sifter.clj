(ns flare.sifter
  (:require 
    [flare.db]
    [datomic.api :as d]))

(def sifting-constant ::master-sifter)

;;; TODO: Consider moving this to flare.event
(defn triggering-attr-eids
  "Returns all attributes that trigger an event."
  [db-conn]
  (set
    (map first
         (d/q '{:find [?t-attrs]
                :where [[_ :event.type/triggering-attrs ?t-attrs]]}
              (d/db db-conn)))))

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

(defn datomic-tx-list-since
  "This is a fn that takes in 3 args and returns a set of entities that have
  been upserted on since the last time we walked through the transaction log.
  
  db-conn is a datomic connection.

  since-t is a transaction id or java.util.Date instant representing the last
  time the transaction log was walked.

  triggering-attrs is a seq of entity ids or :db/idents on attributes that
  generate events."
  [db-conn since-t triggering-attrs]
  (let [log (d/log db-conn)
        now-t (java.util.Date. (+ (System/currentTimeMillis) 100000))]
    (d/q
      '{:find [?tx ?e ?a ?v]
        :in [?log ?last-t ?up-to-t ?attr-set]
        :where [[(tx-ids ?log ?last-t ?up-to-t) [?tx ...]]
                [(tx-data ?log ?tx) [[?e ?a ?v _ ?u]]]
                [(contains? ?attr-set ?a)]]}
      (d/log db-conn) since-t now-t triggering-attrs)))


