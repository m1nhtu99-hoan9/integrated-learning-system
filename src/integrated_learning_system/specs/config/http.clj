(ns integrated-learning-system.specs.config.http
  (:require
    [clojure.spec.alpha :as s]
    [io.pedestal.http :as http-server]
    [integrated-learning-system.spec :as shared-spec]))

(s/def ::http-server/routes vector?)
(s/def ::http-server/type #{:jetty :tomcat})
(s/def ::http-server/join? boolean?)
(s/def ::http-server/port ::shared-spec/port)

(s/def ::config-map
  (s/keys :req [::http-server/join? ::http-server/routes ::http-server/type ::http-server/port]))