(ns integrated-learning-system.components.database
  (:require
    [clojure.spec.alpha :as s]
    [com.brunobonacci.mulog :as mulog]
    [next.jdbc :as jdbc])
  (:import (com.stuartsierra.component Lifecycle)))

(s/def ::log-event #{::init-failed ::init-successfully ::closed ::close-failed})

(defrecord DatabaseComponent [db-cfgmap db-conn]
  Lifecycle

  (start [this]
    (try
      (when-let [db-conn (jdbc/get-connection db-cfgmap)]
        (mulog/log ::init-successfully :instance db-conn)
        (assoc this :db-conn db-conn))
      (catch Exception exn
        (mulog/log ::init-failed :exception exn)
        (throw exn))))

  (stop [this]
    (if (some? db-conn)
      (try
          (.close db-conn)
          (mulog/log ::closed)
          (assoc this :db-conn nil)
        (catch Exception exn
          (mulog/log ::close-failed :exception exn)
          (throw exn))))))

(defn database-component [postgres-cfgmap]
  (DatabaseComponent. (assoc postgres-cfgmap :dbtype "postgresql") nil))
