(ns integrated-learning-system.db.students
  (:require
    [com.brunobonacci.mulog :as mulog]
    [hugsql.core :as hugsql]
    [integrated-learning-system.db :as db]
    [integrated-learning-system.db.sql.commons :refer [path-to-sql]]
    [integrated-learning-system.utils.throwable :refer [exn->map]]
    [next.jdbc.sql :as sql])
  (:import (java.util UUID)))


(defn all-students [db-conn]
  (comment "this fn gonna be redefined by hugsql."))
(defn student-by-username [db-conn {:keys [username]}]
  (comment "this fn gonna be redefined by hugsql."))
(hugsql/def-db-fns (path-to-sql "students"))


(defn add-student! [db-conn
                    {:keys [account-id]}]
  (try
    (sql/insert! (db/with-snake-kebab-opts db-conn)
                 :student
                 {:id (UUID/randomUUID) :account-id account-id})
    (catch Exception exn
      (mulog/log ::failed-add-student!
                 :exn (exn->map exn #(->> % (take 8) (into [])))
                 :account-id account-id)
      ; rethrows to let next.jdbc handle rollback of transaction, if any
      (throw exn))))
