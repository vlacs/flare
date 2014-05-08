(ns flare.event
  (:require [datomic.api :as d]
            [flare.db]))

(defn event-type-entity-id
  "Returns an entity id for a currently registered event."
  [db-conn event-type]
  (first
    (first
      (d/q '[:find ?e
             :in $ ?type
             :where [?e :db/ident ?type]]
           (d/db db-conn)
           event-type))))

(defn registered?
  "Checks to see if a particular event is registered with flare."
  [db-conn event-type]
  (not (nil? (event-type-entity-id db-conn event-type))))

(defn register!
  "Makes flare aware of a particular event so subscribers can subscribe to it."
  [db-conn event-type]
  (if-let [tx-result @(flare.db/tx-entity!
                        db-conn
                        :event
                        {:event/type event-type})]
    (event-type-entity-id db-conn event-type)
    nil))

(defn event!
  "Asserts a fact about a particular event."
  [db-conn event-type event-version user-responsible users-affected message payload]
  (if-let [eteid (event-type-entity-id event-type)]
    (flare.db/tx-entity!
      db-conn
      :event
      (flare.db/strip-nils
        {:assertion/type eteid
         :assertion/event-version event-version
         :assertion/users-affected users-affected
         :assertion/user-responsible user-responsible
         :assertion/message message
         :assertion/payload payload}))
    (throw
      (Exception.
        (str "Unable to assert a fact about an event that's not registered."
             "\nEvent: (" event-type ")")))))

