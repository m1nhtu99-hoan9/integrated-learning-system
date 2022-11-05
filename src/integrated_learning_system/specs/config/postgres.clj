(ns integrated-learning-system.specs.config.postgres
  (:require [clojure.spec.alpha :as s]
            [integrated-learning-system.specs :as shared-spec]))

(s/def ::host string?)
(s/def ::port ::shared-spec/port)
(s/def ::user string?)
(s/def ::password string?)
(s/def ::dbname string?)
(s/def ::dbtype string?)
(s/def ::config-map (s/keys :req-un [::host ::port ::user ::password ::dbname]
                            :opt-un [::dbtype]))
