(ns integrated-learning-system.routing.pages.classes
  (:require
    [clojure.spec.alpha :as s]
    [integrated-learning-system.specs.requests.classes :as s-classes]
    [integrated-learning-system.handlers.webpages.classes :as h]))


(def classes-routes
  ["/classes"
   ["/{class-name}"
    {:parameters {:path (s/keys :req-un [::s-classes/class-name]
                                :opt-un [::s-classes/date ::s-classes/slot-no])}}
    ["/_actions"
     {:conflicting true}
     ["/manage-class-members" {:name    ::class-members-add-or-update
                               :handler h/serve-manage-class-members-page}]
     ["/organise-weekly-schedule" {:name    ::class-periods-add-request
                                   :handler h/serve-organise-schedule-page}]]
    ["/{date}/{slot-no}"
     {:conflicting true}
     ["/homework"
      ["/_actions"
       ["/add" {:name    ::homework-add-request
                :handler h/serve-manage-homework-page}]]]]]])
