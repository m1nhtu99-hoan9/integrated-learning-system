(ns integrated-learning-system.db.teachers
  (:require
    [com.brunobonacci.mulog :as mulog]
    [hugsql.core :as hugsql]
    [integrated-learning-system.db :as db]
    [integrated-learning-system.db.sql.commons :refer [path-to-sql]]
    [integrated-learning-system.utils.throwable :refer [exn->map]]
    [next.jdbc.sql :as sql])
  (:import [java.util UUID]))


(defn all-teachers [db-conn]
  (comment "this fn gonna be redefined by hugsql."))
(defn teacher-by-username [db-conn {:keys [username]}]
  (comment "this fn gonna be redefined by hugsql."))
(hugsql/def-db-fns (path-to-sql "teachers"))


(defn add-teacher! [db-conn
                    {:keys [account-id]}]
  (try
    (sql/insert! (db/with-snake-kebab-opts db-conn)
                 :teacher
                 {:id (UUID/randomUUID) :account-id account-id})
    (catch Exception exn
      (mulog/log ::failed-add-teacher!
                 :exn (exn->map exn #(->> % (take 8) (into [])))
                 :account-id account-id)
      ; rethrows to let next.jdbc handle rollback of transaction, if any
      (throw exn))))
