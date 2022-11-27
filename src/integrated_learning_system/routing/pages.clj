(ns integrated-learning-system.routing.pages
  (:require
    [integrated-learning-system.handlers.webpages :as h]
    [integrated-learning-system.routing.pages.classes :refer [classes-routes]]
    [integrated-learning-system.routing.pages.students :refer [student-routes]]
    [integrated-learning-system.routing.pages.teachers :refer [teacher-routes]]
    [integrated-learning-system.routing.pages.timetable :refer [timetable-routes]]))


(defn webpage-routes []
  ["" {:no-doc true}
   ["/home" h/serve-home-page]
   student-routes
   teacher-routes
   timetable-routes
   classes-routes
   ["/404" h/serve-page-404]
   ["/" h/serve-home-page]])
