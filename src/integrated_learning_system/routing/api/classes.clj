(ns integrated-learning-system.routing.api.classes
  (:require
    [integrated-learning-system.specs.requests.classes :as s-classes]
    [integrated-learning-system.handlers.classes :as h]))

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
     ["/actions"
      ["/move" {:post {:name    ::class-period-move-request
                       :summary "Move a class period to different timeslots"
                       :handler h/move-class-period}}]]
     ["/" {:get {:name    ::class-periods-get-request
                 :summary "Query class periods"
                 :handler h/get-class-periods}}]
     ["/batch"
      {:post {:name    ::class-periods-add-request
              :summary "Organise schedule for class"
              :handler h/organise-class-periods}}]]

    ["/members"
     {:get {:name    ::class-members-get-request
            :summary "Query class teacher and all student(s)"
            :handler h/get-class-members},
      :put {:name    ::class-members-put-request
            :summary "Manage class teacher and students"
            :handler h/replace-class-members}}]]])
