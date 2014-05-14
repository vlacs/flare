(ns flare.util)

(defn required
  "Makes sure that the output of a fn in not nil. If it is nil the only
  option is to throw an exception."
  ([prepared-fn ex-text]
   (required prepared-fn ex-text))
  ([prepared-fn ex-text extra-data]
   (if-let [r (prepared-fn)]
     r (throw (ex-info ex-text extra-data)))))
