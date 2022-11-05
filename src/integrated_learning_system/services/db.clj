(ns integrated-learning-system.services.db
  (:require
    [clojure.spec.alpha :as s]
    [integrant.core :as ig]
    [integrated-learning-system.specs.config.migrations :as s-migrations]
    [integrated-learning-system.specs.config.postgres :as s-postgres]
    [hugsql.core :as hugsql]
    [hugsql.adapter.next-jdbc :refer [hugsql-adapter-next-jdbc]]
    [next.jdbc :as jdbc])
  (:import (javax.sql DataSource)))


(s/def ::log-event #{::init-failed ::init-successfully ::closed ::close-failed})

;; db/postgres

(defmethod ig/prep-key :db/postgres
  [_ cfgmap]
  (assoc cfgmap :dbtype "postgresql"))

(defmethod ig/init-key :db/postgres
  ^DataSource [_ cfgmap]

  ; enables hugsql & next.jdbc interoperability
  (hugsql/set-adapter! (hugsql-adapter-next-jdbc jdbc/unqualified-snake-kebab-opts))

  ; CAVEAT: The returned DataSource cannot be extended with customisable options. See:
  ;   https://github.com/seancorfield/next-jdbc/blob/develop/doc/getting-started.md#datasources-connections--transactions

  ; returns the plain Java DataSource
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
