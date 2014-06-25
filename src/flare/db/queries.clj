(ns flare.db.queries)

(def subscriptions-by-event
  '{:find [?subscription ?inactive ?paused]
    :in [$ % ?event-type]
    :where [(subscriptions-on-type-with-client
              ?subscription ?event-type ?client)
            (subscription-defaults ?subscription ?sub-inactive ?sub-paused)
            (client-defaults ?client ?client-inactive ?client-paused)
            [(or ?sub-inactive ?client-inactive) ?inactive]
            [(or ?sub-paused ?client-paused) ?paused]]})

(def processable-subscriptions-by-event
  '{:find [?subscription]
    :in [$ % ?event-type]
    :where [(subscriptions-on-type-with-client
              ?subscription ?event-type ?client)
            (subscription-defaults ?subscription ?sub-inactive false)
            (client-defaults ?client ?client-inactive false)]})

(def active-subscriptions-by-event
  '{:find [?subscription]
    :in [$ % ?event-type]
    :where [(subscriptions-on-type-with-client
              ?subscription ?event-type ?client)
            (subscription-defaults ?subscription false ?sub-paused)
            (client-defaults ?client false ?client-paused)]})
 
(def subscription-entity-id
  '{:find [?subscription]
    :in [$ % ?client-name ?event-type]
    :where
    [[?client :client/name ?client-name]
     [?event :db/ident ?event-type]
     (subscriptions-on-type-with-client
       ?subscription ?event-type ?client)]})

(def client-entity-id
  '{:find [?e] :in [$ ?name]
    :where [[?e :client/name ?name]]})

(def subscriptions-notifications
  '{:find [?sn-eid ?url ?http-method ?format ?payload]
    :in [$ % ?event-eid]
    :where [[?sn-eid :subscriber-notification/event ?event-eid]
            [?sn-eid :subscriber-notification/subscription ?sub]
            [?sn-eid :subscriber-notification/event ?event]
            [?sub :subscription/url ?url]
            [?sub :subscription/http-method ?http-method]
            [?sub :subscription/format ?format]
            [?event :event/payload ?payload]]})

(def pending-subscription-notifications
  '{:find [?subscriber-notification-eid]
    :in [$ % ?client-eid]
    :where [[?subscriber-notification-eid :subscriber-notification/subscription ?sub]
            [(missing? $ ?subscriber-notification-eid :subscriber-notification/thread-batch)]
            (subscriber-notification-defaults ?subscriber-notification-eid false)
            [?subscriber-notification-eid :subscriber-notification/event ?event]
            (subscription-defaults ?sub ?sub-inactive false)
            (?sub :subscription/client ?client-eid) 
            (client-defaults ?client-eid ?cli-inactive false)]})

(def subscriber-notification-entities-by-batch
  '{:find [?e]
    :in [$ ?batch-eid]
    :where [[?e :subscriber-notification/thread-batch ?batch-eid]]})

(def notifications-by-batch
  '{:find [?notification
           ?url ?auth-token
           ?http-method ?format]
    :in [$ % ?batch-eid]
    :where [[?notification :subscriber-notification/thread-batch ?batch-eid]
            [?evt-eid :subscriber-notification/event ?notification]
            [?sub-eid :subscriber-notification/subscription ?notification]
            [?cli-eid :subscription/client ?sub-eid]
            (subscription-defaults ?cli-eid false false)
            (client-defaults ?cli-eid false false)
            [?auth-token :client/auth-token ?cli]
            [?payload :event/payload ?evt-eid]
            [?version :event/version ?evt-eid]
            [?url :subscription/url ?sub-eid]
            [?http-method :subscription/http-method ?sub-eid]
            [?format :subscription/format ?sub-eid]]})

