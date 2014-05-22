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
            (subscription-defaults ?subscription ?sub-inactive ?sub-paused)
            (client-defaults ?client ?client-inactive ?client-paused)
            [(or ?sub-paused ?client-paused) ?paused]  
            [(= ?paused false)]]})

(def active-subscriptions-by-event
  '{:find [?subscription]
    :in [$ % ?event-type]
    :where [(subscriptions-on-type-with-client
              ?subscription ?event-type ?client)
            (subscription-defaults ?subscription ?sub-inactive ?sub-paused)
            (client-defaults ?client ?client-inactive ?client-paused)
            [(or ?sub-inactive ?client-inactive) ?inactive]  
            [(= ?inactive false)]]})
 
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

(def pending-subscription-notifications
  '{:find [?tx-t
           ?sn
           ?payload
           ?url
           ?http-method
           ?format]
    :in [$ %]
    :where [[?sn :subscriber-notification/subscription ?sub]
            [?sn :subscriber-notification/event ?event ?note-tx]
            [?note-tx :db/txInstant ?tx-t]
            (subscriber-notification-defaults ?sn ?done)
            (subscription-defaults ?sub ?inactive ?paused)
            [(= ?paused false)]
            [(= ?done false)]
            [?event :event/payload ?payload]   
            [?sub :subscription/url ?url]
            [?sub :subscription/http-method ?http-method-enum]
            [?sub :subscription/format ?format-enum]
            [?http-method-enum :db/ident ?http-method]
            [?format-enum :db/ident ?format]]})

