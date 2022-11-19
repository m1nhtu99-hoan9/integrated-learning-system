(ns integrated-learning-system.routing.api
  (:require
    [integrated-learning-system.routing.api.accounts :refer [v1-accounts-routes]]
    [integrated-learning-system.routing.api.classes :refer [v1-classes-routes]]
    [integrated-learning-system.routing.api.courses :refer [v1-courses-routes]]
    [integrated-learning-system.routing.api.students :refer [v1-students-routes]]
    [integrated-learning-system.routing.api.teachers :refer [v1-teachers-routes]]
    [integrated-learning-system.routing.api.timeslots :refer [v1-timeslots-routes]]))


(defn api-v1-routes []
  ["/api/v1"
   (v1-accounts-routes)
   (v1-classes-routes)
   (v1-courses-routes)
   (v1-students-routes)
   (v1-teachers-routes)
   (v1-timeslots-routes)])
