(ns integrated-learning-system.routing.api.classes
  (:require [integrated-learning-system.handlers.classes :as h]))


(defn v1-classes-routes []
  ["/classes"
   {:swagger {:tags ["classes"]}}

   ["/"
    {:post {:name ::class-add-request
            :summary "Create new class for a course"
            :handler h/create-class}}]

   ["/:class-name"
    ["/periods"
     ["/actions"
      ["/move" {:post {:name ::class-period-move-request
                       :summary "Move a class period to different timeslots"}}]]
     ["/batch"
      {:post {:name ::class-periods-add-request
              :summary "Organise class periods for class"}}]]
    ["/teacher"
     {:post {:name ::teacher-class-add-request
             :summary "Assign teacher to class"}}]
    ["/students"
     {:post {:name ::student-class-add-request
             :summary "Add student to class"}}]]])
