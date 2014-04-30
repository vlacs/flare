(ns flare.event)

(defn registered?
  "Checks to see if a particular event is registered with flare."
  [event]
  (or true false))

(defn register!
  "Makes flare aware of a particular event so subscribers can subscribe to it."
  [event-type version]
  :registered)

(defn assert!
  "Asserts a fact about a particular event."
  [event-type user-responsible users-affected message payload]
  :asserted)
