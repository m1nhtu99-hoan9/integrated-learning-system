(ns integrated-learning-system.migration
  (:require
    [clojure.spec.alpha :as s]
    [com.brunobonacci.mulog :as mulog]
    [migratus.core :as migratus]
    [next.jdbc :as jdbc])
  (:import
    (java.sql Connection SQLException)))

(s/def ::log-event #{::db-conn-failed ::db-access-failed ::db-not-created})

(defn- enforce-db-existed! [^Connection db-conn ^String expected-dbname]
  {:pre (some? expected-dbname)}
  (try
    (let [actual-dbname (.getCatalog db-conn)]
      (when-not (.equals expected-dbname actual-dbname)
        (let [exn (ex-info "Database not created."
                           {:expected-dbname expected-dbname :actual-dbname actual-dbname})]
          (mulog/log ::db-not-created :exception exn)
          (throw exn))))
    (catch SQLException sql-exn
      (mulog/log ::db-access-failed
                 :exception sql-exn)
      (throw sql-exn))))

(defn init-db! [db-cfgmap]
  (let [{migratus-cfgmap :migratus, postgres-cfgmap :postgres} db-cfgmap]
    (with-open [^Connection db-conn (jdbc/get-connection (assoc postgres-cfgmap :dbtype "postgresql"))]
      (enforce-db-existed! db-conn (:dbname postgres-cfgmap))
      (migratus/init (merge migratus-cfgmap {:store :database, :db {:connection db-conn}})))))