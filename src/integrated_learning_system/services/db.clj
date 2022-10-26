(ns integrated-learning-system.services.db
  (:require
    [clojure.spec.alpha :as s]
    [integrant.core :as ig]
    [integrated-learning-system.specs.config.migrations :as s-migrations]
    [integrated-learning-system.specs.config.postgres :as s-postgres]
    [hugsql.core :as hugsql]
    [hugsql.adapter.next-jdbc :refer [hugsql-adapter-next-jdbc]]
    [next.jdbc :as jdbc]
    [next.jdbc.result-set :as rs])
  (:import (javax.sql DataSource)))


(s/def ::log-event #{::init-failed ::init-successfully ::closed ::close-failed})

;; db/postgres

(defmethod ig/prep-key :db/postgres
  [_ cfgmap]
  (assoc cfgmap :dbtype "postgresql"))

(defmethod ig/init-key :db/postgres
  ^DataSource [_ cfgmap]

  (hugsql/set-adapter! (hugsql-adapter-next-jdbc {:builder-fn rs/as-unqualified-lower-maps}))
  (jdbc/get-datasource cfgmap))

(defmethod ig/pre-init-spec :db/postgres
  [_]
  ::s-postgres/config-map)


;; db/migrations (to be implemented...)

(defmethod ig/init-key :db/migrations
  [_ cfgmap]
  cfgmap)

(defmethod ig/pre-init-spec :db/migrations
  [_]
  ::s-migrations/config-map)
