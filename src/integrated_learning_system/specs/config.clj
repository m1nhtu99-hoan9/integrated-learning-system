(ns integrated-learning-system.specs.config
  (:require
    [clojure.spec.alpha :as s]
    [integrated-learning-system.specs.config.app :as s-app]
    [integrated-learning-system.specs.config.http :as s-http]
    [integrated-learning-system.specs.config.migrations :as s-migrations]
    [integrated-learning-system.specs.config.postgres :as s-postgres]))

(s/def ::http ::s-http/config-map)
(s/def ::app ::s-app/config-map)
(s/def ::server (s/keys :req-un [::http ::app]))

(s/def ::postgres ::s-postgres/config-map)
(s/def ::migrations ::s-migrations/config-map)
(s/def ::db (s/keys :req-un [::postgres ::migrations]))

(s/def ::config-map (s/keys :req-un [::server ::db]))
