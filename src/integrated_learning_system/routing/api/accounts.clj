(ns integrated-learning-system.routing.api.accounts
  (:require [integrated-learning-system.handlers.accounts :as h]))

(defn v1-accounts-routes []
  ["/accounts"
   {:swagger {:tags ["accounts"]}}

   ["/"
    {:post {:name    ::account-add-request
            :summary "Register new account"
            :handler h/register-account}}]

   ["/{username}"
    {:get {:name    ::get-account-by-username
           :summary "Account by username"
           :handler h/get-account-by-username}}]])
