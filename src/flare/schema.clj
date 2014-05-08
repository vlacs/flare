(ns flare.schema
  (:require [datomic-schematode.constraints :as constraints]))

(def schema
  [;;; A description of something that changes.
   [:event {:attrs [[:type :enum [:ping] :indexed]
                    [:version :keyword]
                    [:users-affected :ref :many]
                    [:user-responsible :ref :one]
                    [:message :string]
                    [:payload :string]]}]

   ;;; Describes clients that can subscribe and their credentials.
   ;;; These should be manually entered.
   [:client {:attrs [[:name :string :db.unique/identity]
                     [:auth-token :string]]}]

   ;;; A client who wants to know about something that happens (an event) and
   ;;; how to tell them about the change.
   [:subscription {:attrs [[:client :ref :indexed]
                           [:event.type :ref :indexed]
                           [:url :string]
                           [:http-method :enum [:put :post]]
                           [:format :enum [:json :edn]]]}]

   ;;; A subscriber notification describes what subscription is being told about
   ;;; a particular assertion and whether or not it has been delivered.
   [:subscriber-notification {:attrs [[:event :ref]
                                      [:subscription :ref]
                                      [:delivered :boolean]]}]  

   ;;; Analagous to subcriber notification, but we don't need to deliver the
   ;;; notification. We wait until a user dismisses it.
   [:user-notification {:attrs [[:event :ref]
                                [:user :ref]
                                [:dismissed :boolean]]}]])

