(ns integrated-learning-system.services.db
  (:require
    [clojure.spec.alpha :as s]
    [com.brunobonacci.mulog :as mulog]
    [next.jdbc :as jdbc]
    [integrant.core :as ig]
    [integrated-learning-system.specs.config.postgres :as s-postgres]
    [integrated-learning-system.specs.config.migrations :as s-migrations]))

(s/def ::log-event #{::init-failed ::init-successfully ::closed ::close-failed})

(defn init-db-conn [postgres-cfg]
  (try
    (when-let [db-conn (jdbc/get-connection postgres-cfg)]
      (mulog/log ::init-successfully :instance db-conn)
      db-conn)
    (catch Exception exn
      (mulog/log ::init-failed :exception exn)
      (throw exn))))

(defn halt-db-conn [db-conn]
  (if (some? db-conn)
    (try
      (.close db-conn)
      (mulog/log ::closed)

      (catch Exception exn
        (mulog/log ::close-failed :exception exn)
        (throw exn)))))


;; db/postgres

(defmethod ig/init-key :db/postgres
  [_ cfgmap]
  (assoc cfgmap :dbtype "postgresql"))

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
