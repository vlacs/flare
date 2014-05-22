(ns flare.api.in
  (:require 
    [flare.util :refer [->json]]
    [liberator.core :refer [defresource]]))

(def malformed-response
  {:status 422
   :headers {}
   :body (->json {:error "Recieved valid but unexpected json data."})})

(def success-response
  {:status 200
   :headers {}
   :body (->json {:success true})})

#_(defn make-resources
  [db-conn]
  [(resource pause-subscription []
             :allowed-methods [:put :post]
             :available-media-types ["application/json"]
             :malformed?
             :handle-malformed
             :handle-ok success-response
             :post! (fn [ctx] nil)
             )])
