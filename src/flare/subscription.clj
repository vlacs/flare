(ns flare.subscription
  (:require [flare.state :as state]))

(defn add-client!
  [client-name auth-token]
  (state/tx-entity!
    flare.state/db
    :client
    {:client/name client-name
     :client/auth-token auth-token}))

(defn remove-client!
  []
  :removed
  )

(defn subscribe!
  []
  :subscribed
  )

(defn unsubscribe!
  ([subscription-id]
   :a-specific-subscription)
  ([client event]
   :a-deterministic-subscription))
