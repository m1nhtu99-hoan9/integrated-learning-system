(ns integrated-learning-system.routing.pages.students
  (:require [integrated-learning-system.handlers.webpages.students :as h]))


(def student-routes
  ["/students"
   {:name       ::all-students-page-request
    :handler h/serve-all-student-pages}])