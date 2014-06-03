(ns flare.web)

(defn helmsman-definition [system]
  (let [db-conn (:db-conn system)]
    [[:put "/subscribe" :a #_flare/subscribe]
     [:put "/unsubscribe" :b #_flare/unsubscribe]]))
