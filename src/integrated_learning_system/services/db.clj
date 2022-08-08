(ns integrated-learning-system.services.db
  (:require
    [clojure.spec.alpha :as s]
    [com.brunobonacci.mulog :as mulog]
    [next.jdbc :as jdbc]
    [next.jdbc.specs :as s-jdbc]
    [integrant.core :as ig]
    [integrated-learning-system.specs.config.postgres :as s-postgres]
    [integrated-learning-system.specs.config.migrations :as s-migrations])
  (:import (java.sql Connection)
           (javax.sql DataSource)))


(s/def ::log-event #{::init-failed ::init-successfully ::closed ::close-failed})


(defn init-db-conn [postgres-cfg]
  (try
    (when-let [db-conn (jdbc/get-connection postgres-cfg)]
      (mulog/log ::init-successfully :instance db-conn)
      ^Connection db-conn)
    (catch Exception exn
      (mulog/log ::init-failed :exception exn)
      (throw exn))))

(s/fdef init-db-conn
  :args (s/cat :postgres-cfg (s/or :spec ::s-jdbc/db-spec-map
                                   :datasource ::s-jdbc/datasource)))


(defn halt-db-conn [^Connection db-conn]
  (if (some? db-conn)
    (try
      (.close db-conn)
      (mulog/log ::closed)

      (catch Exception exn
        (mulog/log ::close-failed :exception exn)
        (throw exn)))))


;; db/postgres

(defmethod ig/prep-key :db/postgres
  [_ cfgmap]
  (assoc cfgmap :dbtype "postgresql"))

(defmethod ig/init-key :db/postgres
  ^DataSource [_ cfgmap]
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
