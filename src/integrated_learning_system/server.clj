(ns integrated-learning-system.server
  (:require [clojure.spec.alpha :as s]
            [com.brunobonacci.mulog :as mulog]
            [integrated-learning-system.services :refer [config->service-map]]))

(s/def ::state #{:started :resumed :stopped :restarted :failed})
(s/def ::env #{:prod :dev :test})

(mulog/start-publisher! {:type :console})

(defn -main [config-fname]
  (config->service-map config-fname))

(comment
  (-main "config.edn"))