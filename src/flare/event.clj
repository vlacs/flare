(ns flare.event
  (:require [datomic.api :as d]
            [flare.db]
            [hatch]))

(defn app-event-keyword
  [app event]
  (keyword (str (name app) "." (name event))))

(defn slam-event-type
  "Puts an event type into our special event-type namespace with application
  segregation so different applications can have event-types with the same
  names. Every event is application namespaced."
  [application event-type]
  (flare.db/enum-keyword :event :type (app-event-keyword application event-type)))

(defn registered?
  "Checks to see if a particular event is registered with flare."
  [db-conn application event-type]
  (not (nil? (d/entity (d/db db-conn) event-type))))

(defn register!
  "Makes flare aware of a particular event so subscribers can subscribe to it."
  [db-conn application event-type]
  (when @(flare.db/tx-entity!
           db-conn
           :event.type
           {:db/ident (slam-event-type
                        application
                        event-type)})
    (slam-event-type application event-type)))

(def query-subscriptions
  '[:find ?e ?sub-inactive ?client-inactive
    :in $ ?event-type
    :where 
    [?e :subscription/event.type ?event-type]
    [?e :subscription/client ?client]
    [(get-else $ ?e :subscription/inactive false) ?sub-inactive]
    [(get-else $ ?client :client/inactive? false) ?client-inactive]])

(defn make-subscription-notifications
  [event-type event-tempid]
  [:foo]
  )

(defn make-user-notifications
  [event-type event-tempid]
  [:bar]
  )

(defn make-notifications
  [event-type event-tempid]
  (vec
    (concat
      (make-subscription-notifications event-type event-tempid)
      (make-user-notifications event-type event-tempid))))

(defn event!
  "Prepares datoms describing the event to be transacted in with all of the
  notifications associated with it as of the time event! was called."
  [db-conn event-type event-version user-responsible users-affected message payload]
  (let [event-tempid (flare.db/new-tempid :event)
        clean-event (->> {:event/type event-type
                          :event/version event-version
                          :event/users-affected users-affected
                          :event/user-responsible user-responsible
                          :event/message message
                          :event/payload payload}
                         (flare.db/clean-entity :event)
                         flare.db/strip-nils)]
    (concat
      [clean-event]
      (make-notifications event-type event-tempid))))

