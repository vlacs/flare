(ns flare.schema
  (:require [datomic-schematode.constraints :as constraints]))

(def schema
  [;;; A description of something that changes.
   ;; the calling aplication hands this structure to flare:

   ;;; We need a list of all the types we have.
   ;;; This has a fully qualified keyword :db/ident value that is associated
   ;;; with it but isn't shown here.
   ;;; TODO: Ask Matt if we can put [:db/ident :keyword] before :application.
   [:event.type {:attrs [[:application :keyword]
                         [:name :keyword]
                         [:description :string]]}]

   [:event {:attrs [[:type :enum [:flare.ping] :indexed]
                    [:version :keyword]
                    [:users-affected :ref :many]
                    [:user-responsible :ref :one]
                    [:message :string]
                    [:payload :string]]}]

   ;;; Describes clients that can subscribe and their credentials.
   ;;; These should be manually entered.
   [:client {:attrs [[:name :string :db.unique/identity]
                     [:auth-token :string]
                     [:paused? :boolean] ;;; Not processing any notifications.
                     [:inactive? :boolean] ;;; Not making any notifications.
                     ;;; consider a [:delay :int] to globally rate-limit updates
                     ;;; to any particular client (allow bursting for similar
                     ;;; updates).
                     ]}]

   ;;; A client who wants to know about something that happens (an event) and
   ;;; how to tell them about the change.
   [:subscription {:attrs [[:client :ref :indexed]
                           [:event.type :ref :indexed]
                           [:url :string]
                           [:http-method :enum [:put :post]]
                           [:format :enum [:json :edn]]
                           [:paused? :boolean] ;;; Not processing these notifications.
                           [:inactive? :boolean] ;;; Not making these notifications.
                           ;;; See possible client.delay attr. We might want an
                           ;;; overriding one here also a burst size if the sub
                           ;;; supports multiple updates per api call.
                           ] 
                   :dbfns [(constraints/unique :subscription :client :event.type)]}]

   ;;; A subscriber notification describes what subscription is being told about
   ;;; a particular assertion and whether or not it has been delivered.
   [:subscriber-notification {:attrs [[:event :ref]
                                      [:subscription :ref]
                                      [:attempt-instants :instant :many]
                                      [:last-http-status :bigint]
                                      [:last-reply-body :string]
                                      ;; https://groups.google.com/forum/#!topic/datomic/p3FLisquFH8
                                      ;; use a datomic rule to default to false for
                                      ;; :done? when it's not set
                                      [:done? :boolean]]
                              :dbfns [(constraints/unique
                                        :subscriber-notification
                                        :event :subscription)]}]  

   ;;; Analagous to subcriber notification, but we don't need to deliver the
   ;;; notification. We wait until a user dismisses it.
   [:user-notification {:attrs [[:event :ref]
                                [:user :ref]
                                [:dismissed? :boolean]]
                        :dbfns [(constraints/unique
                                  :user-notification
                                  :event :user)]}]])

