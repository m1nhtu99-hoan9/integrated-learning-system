(ns integrated-learning-system.server
  (:require [clojure.spec.alpha :as s]
            [com.brunobonacci.mulog :as mulog]
            [com.brunobonacci.mulog.core :refer [publishers]]
            [integrated-learning-system.services :refer [config->service-map]])
  (:import [com.brunobonacci.mulog.publisher ConsolePublisher]))


(s/def ::state #{:started :resumed :stopped :restarted :failed})
(s/def ::env #{:prod :dev :test})

(if [not-any? #(instance? ConsolePublisher %) @publishers]
  (mulog/start-publisher! {:type :console, :pretty? true}))

(defn -main [config-fname]
  (config->service-map config-fname))

(comment
  (-main "config.edn"))