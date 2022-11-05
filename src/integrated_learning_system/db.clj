(ns integrated-learning-system.db
  (:require [camel-snake-kebab.extras :as cske]
            [camel-snake-kebab.core :as csk]
            [com.brunobonacci.mulog :as mulog]
            [next.jdbc :as jdbc]
            [next.jdbc.specs :as s-jdbc]
            [clojure.spec.alpha :as s])
  (:import (java.sql Connection)))

(defn init-db-conn [postgres-cfg]
  (try
    (when-let [db-conn (jdbc/get-connection postgres-cfg)]
      (mulog/log ::db-conn-init-successfully :instance db-conn)
      ^Connection db-conn)
    (catch Exception exn
      (mulog/log ::failed-init-db-conn :exception exn)
      nil)))

(s/fdef init-db-conn
        :args (s/cat :postgres-cfg (s/or :spec ::s-jdbc/db-spec-map
                                         :datasource ::s-jdbc/datasource)))


(defn halt-db-conn [^Connection db-conn]
  (if (some? db-conn)
    (try
      (.close db-conn)
      (mulog/log ::db-conn-closed)

      (catch Exception exn
        (mulog/log ::failed-halt-db-conn :exception exn)
        (throw exn)))))

(defn with-snake-kebab-opts [connectible]
  (jdbc/with-options connectible jdbc/unqualified-snake-kebab-opts))

(defn transform-column-keys [map]
  (cske/transform-keys csk/->snake_case_keyword map))
