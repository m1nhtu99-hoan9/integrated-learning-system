(ns integrated-learning-system.specs.server
  (:require [clojure.spec.alpha :as s]))

(s/def ::env #{:prod :dev :test})
