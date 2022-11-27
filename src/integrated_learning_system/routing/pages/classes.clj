(ns integrated-learning-system.routing.pages.classes
  (:require
    [clojure.spec.alpha :as s]
    [integrated-learning-system.specs.requests.classes :as s-classes]
    [integrated-learning-system.specs.requests.timetable :as s-timetable]
    [integrated-learning-system.handlers.webpages.classes :as h]))


(def classes-routes
  ["/classes"
   ["/" {:name    ::all-classes-page
         :handler h/serve-all-classes-page}]

   ["/{class-name}"
    {:parameters {:path  (s/keys :req-un [::s-classes/class-name]
                                 :opt-un [::s-classes/date ::s-classes/slot-no]),
                  :query (s/keys :opt-un [::s-timetable/year ::s-timetable/week])}}

    ["/timetable"
     {:name    ::class-timetable-page
      :handler h/serve-class-timetable-page}]
    ["/_actions"
     {:conflicting true}
     ["/manage-class-members" {:name    ::class-members-add-or-update-page
                               :handler h/serve-manage-class-members-page}]
     ["/organise-weekly-schedule" {:name    ::class-periods-add-page
                                   :handler h/serve-organise-schedule-page}]]

    ["/{date}/{slot-no}"
     {:conflicting true}
     ["/" {:name        ::class-period-page
           :handler     h/serve-class-period-info-page}]
     ["/homework"
      ["/" {:name    ::class-homework-page
            :handler h/serve-class-homework-page}]
      ["/_actions"
       ["/add" {:name    ::homework-add-page
                :handler h/serve-manage-homework-page}]]]]

    ["/" {:name    ::class-page
          :handler h/serve-class-info-page}]]])
