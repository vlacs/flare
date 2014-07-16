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
  [db-conn application event-type triggering-attrs]
  (let [slammed-event (slam-event-type application event-type)]
    (if @(flare.db/tx-entity!
             db-conn
             :event.type
             {:db/ident slammed-event
              :event.type/application application
              :event.type/name event-type
              :event.type/triggering-attrs triggering-attrs})
      (do
        (timbre/info "New event has been registered: " (str slammed-event))
        slammed-event)
      (timbre/info "New event failed to register: " (str slam-event-type)))))


