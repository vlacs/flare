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
  '{:find [?sn
           ?payload
           ?url
           ?auth-token
           ?http-method
           ?format]
    :in [$ %]
    :where [[?sn :subscriber-notification/subscription ?sub]
            [(missing? $ ?sn :subscriber-notification/thread-batch)]
            (subscriber-notification-defaults ?sn false)
            [?sn :subscriber-notification/event ?event]
            (subscription-defaults ?sub ?sub-inactive false)
            (?sub :subscription/client ?cli) 
            (client-defaults ?cli ?cli-inactive false)
            [?cli :client/auth-token ?auth-token]
            [?event :event/payload ?payload]   
            [?sub :subscription/url ?url]
            [?sub :subscription/http-method ?http-method-enum]
            [?sub :subscription/format ?format-enum]
            [?http-method-enum :db/ident ?http-method]
            [?format-enum :db/ident ?format]]})

(def notifications-by-batch-uuid
  '{:find [?notification
           ?payload ?version
           ?url ?auth-token
           ?http-method ?format]
    :in [$ % ?batch-uuid]
    :where [[?tb :thread-batch/batch ?batch-uuid]
            [?notification :subscriber-notification/thread-batch ?tb]
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

