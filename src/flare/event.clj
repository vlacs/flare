(ns flare.event
  (:require [datomic.api :as d]
            [flare.db]
            [flare.db.queries :as queries]
            [flare.db.rules :as rules]
            [hatch]
            [taoensso.timbre :as timbre]))

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
  (not (nil? (d/entity (d/db db-conn) (slam-event-type application event-type)))))

(defn register!
  "Makes flare aware of a particular event so subscribers can subscribe to it."
  [db-conn application event-type]
  (let [slammed-event (slam-event-type application event-type)]
    (if @(flare.db/tx-entity!
             db-conn
             :event.type
             {:db/ident slammed-event
              :event.type/application application
              :event.type/name event-type})
      (do
        (timbre/debug "New event has been registered: " (str slammed-event))
        slammed-event)
      (timbre/debug "New event failed to register: " (str slam-event-type)))))

(defn new-subscription-notification-instance
  "Creates a notification instance based on a to-be-transacted event by the
  tempid that was generated for it. Notifications should only be made against
  an event that hasn't been transacted yet, hence the tempid."
  [event-tempid subscription-eid]
  (hatch/clean-entity
    flare.db/partitions
    flare.db/valid-attrs
    :subscriber-notification
    (hatch/slam-all
      {:event event-tempid
       :subscription subscription-eid}
      :subscriber-notification)))

(defn make-subscription-notifications
  "Creates subscription-notification entities for subscriptions on a particular
  event type. Notifications are only made at the time the event is being made,
  hence the event-tempid."
  [db-conn event-type event-tempid]
  (let [subscriptions (d/q flare.db.queries/active-subscriptions-by-event
                           (d/db db-conn)
                           flare.db.rules/defaults
                           event-type)]
    (if (not (empty? subscriptions))
      (vec
        (map
          (fn subscription-notification-transformer
            [i] (new-subscription-notification-instance event-tempid (first i)))
          subscriptions))
      [])))

(defn make-user-notifications
  [db-conn event-type event-tempid]
  ;;; TODO: Figure out exactly how this will work.
  []
  )

(defn event
  "Prepares datoms describing the event to be transacted in with all of the
  notifications associated with it as of the time event! was called."
  [db-conn event-type event-version user-responsible users-affected message payload]
  (let [event-tempid (flare.db/new-tempid :event)
        clean-event (->> {:db/id event-tempid
                          :event/type event-type
                          :event/version event-version
                          :event/users-affected users-affected
                          :event/user-responsible user-responsible
                          :event/message message
                          :event/payload payload}
                         (flare.db/clean-entity :event)
                         flare.db/strip-nils)]
    (vec
      (concat
        [clean-event]
        (make-subscription-notifications db-conn event-type event-tempid)
        (make-user-notifications db-conn event-type event-tempid)))))

