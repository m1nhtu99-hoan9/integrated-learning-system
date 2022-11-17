(ns integrated-learning-system.routing.pages.classes
  (:require
    [clojure.spec.alpha :as s]
    [integrated-learning-system.specs.requests.classes :as s-classes]
    [integrated-learning-system.handlers.webpages.classes :as h]))

(def classes-routes
  ["/classes"
   ["/{class-name}"
    ["/_actions"
     ["/organise-weekly-schedule" {:name       ::class-periods-add-request
                                   :parameters {:path (s/keys :opt-un [::s-classes/class-name])}
                                   :handler    h/serve-organise-schedule-page}]]]])
