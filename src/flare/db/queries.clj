(ns flare.db.queries)

(def subscription-by-event
  '{:find [?subscription ?client ?inactive ?paused]
    :in [$ % ?event-type]
    :where [[?subscription :subscription/event.type ?event-type]
            [?subscription :subscription/client ?client]
            (subscription-defaults ?subscription ?sub-inactive ?sub-paused)
            (client-defaults ?client ?client-inactive ?client-paused)
            [(or ?sub-inactive ?client-inactive) ?inactive]
            [(or ?sub-paused ?client-paused) ?paused]]})
 
(def subscription-entity-id
  '{:find [?subscription]
    :in [$ ?client-name ?event-type]
    :where
    [[?client :client/name ?name]
     [?event :event/type ?event-type]
     [?subscription :subscription/event ?event]
     [?subscription :subscription/client ?client]]})

(def client-entity-id
  '{:find [?e] :in [$ ?name]
    :where [[?e :client/name ?name]]})
