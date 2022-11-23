(ns integrated-learning-system.routing.api.classes
  (:require
    [integrated-learning-system.specs.requests.classes :as s-classes]
    [integrated-learning-system.handlers.classes :as h]
    [integrated-learning-system.handlers.classes.periods :as hp]
    [integrated-learning-system.handlers.classes.members :as hm]))


(defn v1-classes-routes []
  ["/classes"
   {:swagger {:tags ["classes"]}}

   ["/"
    {:post {:name    ::class-add-request
            :summary "Create new class for a course"
            :handler h/create-class}}]
   ["/:class-name"
    {:parameters {:path {:class-name ::s-classes/class-name}}}
    ["/periods"
     ["/batch"
      {:post {:name    ::class-periods-add-request
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
