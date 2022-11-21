(ns integrated-learning-system.routing.pages.classes
  (:require
    [clojure.spec.alpha :as s]
    [integrated-learning-system.specs.requests.classes :as s-classes]
    [integrated-learning-system.handlers.webpages.classes :as h]))


(def classes-routes
  ["/classes"
   ["/{class-name}"
    (let [path-params-spec {:path (s/keys :opt-un [::s-classes/class-name])}]
      ["/_actions"
       ["/manage-class-members" {:name       ::class-members-add-or-update
                                 :parameters path-params-spec
                                 :handler    h/serve-manage-class-members-page}]
       ["/organise-weekly-schedule" {:name       ::class-periods-add-request
                                     :parameters path-params-spec
                                     :handler    h/serve-organise-schedule-page}]])]])
