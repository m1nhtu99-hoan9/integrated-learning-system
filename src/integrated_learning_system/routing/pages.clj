(ns integrated-learning-system.routing.pages
  (:require
    [integrated-learning-system.routing.pages.timetable :refer [timetable-route]]
    [integrated-learning-system.handlers.webpages :as h]))


(defn webpage-routes []
  ["" {:no-doc true}
   ["/home" h/serve-home-page]
   timetable-route
   ["/404" h/serve-page-404]
   ["/" h/serve-home-page]])