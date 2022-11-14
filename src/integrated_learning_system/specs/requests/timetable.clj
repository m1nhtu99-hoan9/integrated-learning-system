(ns integrated-learning-system.specs.requests.timetable
  (:require [clojure.spec.alpha :as s]))

(defonce validation-messages
  {::year (fn [query-params]
            (str "Expected an year number not before 2000, but found: " (:year query-params))),
   ::week (fn [query-params]
            (str "Expected a positive number for 'week', but found: " (:week query-params)))})

(s/def ::year (s/and int? #(>= % 2000)))
(s/def ::week pos-int?)
