(ns integrated-learning-system.services.app
  (:require
    [integrant.core :as ig]
    [clojure.pprint :refer [pprint]]
    [clojure.spec.alpha :as s]
    [com.brunobonacci.mulog :as mulog]
    [integrated-learning-system.spec :as shared-spec]
    [integrated-learning-system.server :as server]))

(defmethod ig/init-key :server/app
  [_ {:keys [name version env], :as config}]

  (mulog/set-global-context! {:app-name name
                              :version  version
                              :env      env})
  config)

;; Specs
(s/def ::config-map--name string?)
(s/def ::config-map--version ::shared-spec/semver)
(s/def ::config-map--env ::server/env)
(s/def ::config-map (s/keys :req-un [::config-map--name ::config-map--version ::config-map--env]))
