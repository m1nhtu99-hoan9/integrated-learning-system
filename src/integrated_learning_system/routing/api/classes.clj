(ns integrated-learning-system.routing.api.classes
  (:require
    [clojure.spec.alpha :as s]
    [integrated-learning-system.handlers.classes :as h]
    [integrated-learning-system.handlers.classes.homeworks :as hh]
    [integrated-learning-system.handlers.classes.members :as hm]
    [integrated-learning-system.handlers.classes.periods :as hp]
    [integrated-learning-system.specs.requests.classes :as s-classes]))


(defn v1-classes-routes []
  ["/classes"
   {:swagger {:tags ["classes"]}}

   ["/"
    {:post {:name    ::class-add-request
            :summary "Create new class for a course"
            :handler h/create-class}}]
   ["/:class-name"
    {:parameters {:path (s/keys :req-un [::s-classes/class-name]
                                :opt-un [::s-classes/date ::s-classes/slot-no])}}
    ["/periods"
     ["/{date}/{slot-no}"
      {:conflicting true}
      ["/homework"
       {:post {:name ::homework-add-request
               :summary "Create homework"
               :handler hh/create-homework}}]]
     ["/batch"
      {:conflicting true
       :post {:name    ::class-periods-add-request
              :summary "Organise schedule for class"
              :handler hp/organise-class-periods}}]
     ["/" {:get {:name    ::class-periods-get-request
                 :summary "Query class periods"
                 :handler hp/get-class-periods}}]]

    ["/members"
     {:get {:name    ::class-members-get-request
            :summary "Query class teacher and all student(s)"
            :handler hm/get-class-members},
      :put {:name    ::class-members-put-request
            :summary "Manage class teacher and students"
            :handler hm/replace-class-members}}]]])
