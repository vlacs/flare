(ns flare.schema
  (:require [datomic-schematode.constraints :as constraints]))

(def schema
  [[:event {:attrs [[:type :keyword :indexed]
                    [:version :keyword :indexed]
                    [:users-affected :ref :many]
                    [:user-responsible :ref :one]
                    [:message :string]
                    [:payload-format :enum [:json :edn]]
                    [:payload :string]]}]
   [:subscription {:attrs [[:id-sk :bigint :indexed]
                           [:uri :string :indexed]
                           [:method :enum [:put :post]]
                           [:event-type :keyword]]}]
   [:notification-destination {:attrs [[:name "string" :indexed]
                                       [:type :enum [:user :subscription]]
                                       ;; If a user is the recipient
                                       [:user :ref]
                                       ;; If a remote system is the recipient
                                       [:subscription :ref]]}]
   [:notification {:attrs [[:event :ref]
                           [:destination :ref]
                           ;; TODO: add expiry attr?
                           [:delivered :boolean]]}]])
