(ns integrated-learning-system.routing.pages.teachers
  (:require [integrated-learning-system.handlers.webpages.teachers :as h]))


(def teacher-routes
  ["/teachers"
   {:name    ::all-teachers-page-request
    :handler h/serve-all-teacher-pages}])