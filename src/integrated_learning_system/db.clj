(ns integrated-learning-system.db
  (:require [com.brunobonacci.mulog :as mulog]
            [next.jdbc :as jdbc]
            [next.jdbc.specs :as s-jdbc]
            [clojure.spec.alpha :as s])
  (:import (java.sql Connection)))

(defn init-db-conn [postgres-cfg]
  (try
    (when-let [db-conn (jdbc/get-connection postgres-cfg)]
      (mulog/log ::init-successfully :instance db-conn)
      ^Connection db-conn)
    (catch Exception exn
      (mulog/log ::init-failed :exception exn)
      nil)))

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
