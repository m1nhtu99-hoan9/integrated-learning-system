(ns integrated-learning-system.routing.api.teachers
  (:require [integrated-learning-system.handlers.teachers :as h]))


(defn v1-teachers-routes []
  ["/teachers"
   {:swagger {:tags ["teachers"]}}

   ["/" {:get {:name    ::teachers-get-request
               :summary "Query all teachers"
               :handler h/get-all-teachers}}]])
