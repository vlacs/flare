(ns flare.web)

(def helmsman-definition
  "Main helmsman definition"
  [[:put "/subscribe" :a #_flare/subscribe]
   [:put "/unsubscribe" :b #_flare/unsubscribe]])
