(ns flare.db.rules)

(def defaults
  '[[(subscription-defaults ?subscription ?sub-inactive ?sub-paused)
     [(get-else $ ?subscription :subscription/inactive? false) ?sub-inactive]
     [(get-else $ ?subscription :subscription/paused? false) ?sub-paused]]

    [(client-defaults ?client ?client-inactive ?client-paused)
     [(get-else $ ?client :client/inactive? false) ?client-inactive]
     [(get-else $ ?client :client/paused? false) ?client-paused]]
    
    [(subscriber-notification-defaults ?sn ?done)
     [(get-else $ ?sn :subscriber-notification/done? false) ?done]]])


