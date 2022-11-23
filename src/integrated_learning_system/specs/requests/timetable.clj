(ns integrated-learning-system.specs.requests.timetable
  (:require
    [clojure.spec.alpha :as s]
    [integrated-learning-system.specs.requests.accounts :as s-accounts]))

(defonce validation-messages
  (-> {::year (fn [query-params]
                (str "Expected an year number not before 2000, but found: " (:year query-params))),
       ::week (fn [query-params]
                (str "Expected a positive number, but found: " (:week query-params)))}
      (merge (select-keys s-accounts/validation-messages [::s-accounts/username-chars
                                                          ::s-accounts/username-length
                                                          ::s-accounts/username-nonempty]))))

(s/def ::year (s/and int? #(>= % 2000)))
(s/def ::week pos-int?)
(s/def ::username ::s-accounts/username)
(s/def ::user-role keyword?)
