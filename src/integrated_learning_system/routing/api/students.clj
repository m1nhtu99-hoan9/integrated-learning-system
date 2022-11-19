(ns integrated-learning-system.routing.api.students
  (:require [integrated-learning-system.handlers.students :as h]))


(defn v1-students-routes []
  ["/students"
   {:swagger {:tags ["students"]}}

   ["/" {:get {:name    ::students-get-request
               :summary "Query all students"
               :handler h/get-all-students}}]])
