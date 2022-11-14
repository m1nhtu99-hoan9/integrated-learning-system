(ns integrated-learning-system.routing.pages.timetable
  (:require
    [clojure.spec.alpha :as s]
    [integrated-learning-system.handlers.webpages :as h]
    [integrated-learning-system.specs.requests.timetable :as s-timetable]))

(def timetable-route
  ["/timetable" {:name       ::timetable-page-request
                 :parameters {:query (s/keys :opt-un [::s-timetable/year ::s-timetable/week])}
                 :handler    h/serve-timetable-page}])
