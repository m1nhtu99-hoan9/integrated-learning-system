(ns integrated-learning-system.routing.api.timeslots
  (:require [integrated-learning-system.handlers.timeslots :as h]))


(defn v1-timeslots-routes []
  ["/timeslots"
   {:swagger {:tags ["timeslots"]}}

   ["/"
    {:post {:name ::timeslot-add-request
            :summary "Insert new time-slot"
            :handler h/insert-timeslot}}]])