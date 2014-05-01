(ns flare.event
  (:require [datomic.api :as d]
            [flare.state :as state]))

(defn event-entity-id
  "Returns an entity id for a currently registered event."
  [event-type]
  (first
    (first
      (d/q '[:find ?e
             :in $ ?type
             :where [?e :event/type ?type]]
           (d/db flare.state/db)
           event-type))))

(defn event-version
  "Returns the version of the passed in event keyword or nil if it doesn't
  exist in the database."
  [event-type]
  (first
    (first
      (d/q '[:find ?version
             :in $ ?type
             :where
             [?e :event/type ?type]
             [?e :event/version ?version]]
           (d/db flare.state/db)
           event-type))))

(defn registered?
  "Checks to see if a particular event is registered with flare."
  [event-type]
  (not (nil? (event-entity-id event-type))))

(defn register!
  "Makes flare aware of a particular event so subscribers can subscribe to it."
  [event-type version]
  (if-let [tx-result @(state/tx-entity!
                        flare.state/db
                        :event
                        {:event/type event-type
                         :event/version version})]
    (event-entity-id event-type)
    nil))

(defn assert!
  "Asserts a fact about a particular event."
  [event-type user-responsible users-affected message payload]
  (if-let [eeid (event-entity-id event-type)]
    (state/tx-entity!
      flare.state/db
      :assertion
      (flare.state/strip-nils
        {:assertion/event eeid
         :assertion/event-version (event-version event-type)
         :assertion/users-affected users-affected
         :assertion/user-responsible user-responsible
         :assertion/message message
         :assertion/payload payload}))
    (throw
      (Exception.
        (str "Unable to assert a fact about an event that's not registered."
             "\nEvent: (" event-type ")")))))

