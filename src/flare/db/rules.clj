(ns flare.db.rules)

(def defaults
  '[[(subscription-defaults ?subscription ?sub-inactive ?sub-paused)
     [(get-else $ ?subscription :subscription/inactive? false) ?sub-inactive]
     [(get-else $ ?subscription :subscription/paused? false) ?sub-paused]]

    [(client-defaults ?client ?client-inactive ?client-paused)
     [(get-else $ ?client :client/inactive? false) ?client-inactive]
     [(get-else $ ?client :client/paused? false) ?client-paused]]
    
    [(subscriber-notification-defaults ?sn ?status)
     [(get-else $ ?sn :subscriber-notification/status false) ?status]]
    
    [(subscriptions-on-type-with-client ?sub ?type ?client)
     [?sub :subscription/event.type ?type]
     [?sub :subscription/client ?client]]])


