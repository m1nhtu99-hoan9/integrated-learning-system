(ns integrated-learning-system.specs.config.app
  (:require [clojure.spec.alpha :as s]
            [integrated-learning-system.spec :as shared-spec]
            [integrated-learning-system.specs.server :as s-server]))

(s/def ::name string?)
(s/def ::version ::shared-spec/semver)
(s/def ::env ::s-server/env)
(s/def ::config-map (s/keys :req-un [::name ::version ::env]))