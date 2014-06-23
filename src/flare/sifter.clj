(ns flare.sifter
  (:require [datomic.api :as d]))

(def sifting-constant ::immaconstantthatyoullneverseeanywhereelse)

(defn triggering-attr-eids
  [db-conn]
  nil
  )

(defn set-last-sift!
  [db-conn]
  nil
  )

(defn last-sift-at
  [db-conn]
  nil
  )

(defn reduced-db
  [db-conn last-sift]
  (d/since (d/db db-conn) last-sift))
