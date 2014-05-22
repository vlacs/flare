(ns flare
  (:require [flare.schema :as schema]
            [flare.db]
            [hatch]))

(def schema flare.schema/schema)

#_(defn helmsman
  [db-conn]
  [[:context "api"
    [:context "subscription"
     [:post "new" new-sub]
     [:post "pause" subscription-pause]
     [:post "resume" subscription-resume]]
    [:context "client"
     [:post "pause" client-pause]
     [:post "resume" client-resume]]]])
 
(defn init!
  "Starts up flare for the first time. By default, we don't load up the schema.
  this is because Galleon will do it for us, but in development we will manually
  specify that we want to load it at this point."
  ([system]
   (init! false system))
  ([init-database? system]
   (when init-database?
     (flare.db/init-database! (:db-conn system)))
   system))

(defn start!
  "Brings flare up so we'll be ready for business."
  [system]
  ;;; Start up notification processing.
  system)

(defn stop!
  "Wraps up everything flare is working on so we can cleanly shutdown."
  [system]
  ;;; Shutdown notification processing
  system)


