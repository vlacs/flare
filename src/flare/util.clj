(ns flare.util
  (:require [clojure.java.io :as io]
            [clojure.data.json :as json]
            [clojure.edn :as edn]
            ))

(defn required
  "Makes sure that the output of a fn in not nil. If it is nil the only
  option is to throw an exception."
  ([prepared-fn ex-text]
   (required prepared-fn ex-text {}))
  ([prepared-fn ex-text extra-data]
   (if-let [r (prepared-fn)]
     r (throw (ex-info ex-text extra-data)))))

(defn ->str
  "Tries to make the object a string, such as reading streams and turning
  them into a string."
  [input]
  (condp instance? input
    java.lang.String input
    (slurp (io/reader input))))

(defn json->
  "From json string to clojure data."
  [json-str]
  (json/read-str json-str))

(defn ->json
  "From clojure data to json."
  [jsonable]
  (json/write-str jsonable))

(defn edn->json
  "Converts an EDN payload into a json object."
  [convertable-string]
  (->json (edn/read-string convertable-string)))
 
;; Flare knows what queues we need, but it doesn't [need to know] what worker fns handle the queues.
(defn get-queues [system]
  (assoc-in system [:flare :queues]
            [:genius :moodle :showevidence]))
