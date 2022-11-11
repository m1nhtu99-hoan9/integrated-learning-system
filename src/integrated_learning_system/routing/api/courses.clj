(ns integrated-learning-system.routing.api.courses
  (:require [integrated-learning-system.handlers.courses :as h]))

(defn v1-courses-routes []
  ["/courses"
   {:swagger {:tags ["courses"]}}

   ["/"
    {:post {:name ::course-add-request
            :summary "Create new course"
            :handler h/create-course}}]])
