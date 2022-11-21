(ns integrated-learning-system.db.students
  (:require
    [clojure.spec.alpha :as s]
    [com.brunobonacci.mulog :as mulog]
    [hugsql.core :as hugsql]
    [integrated-learning-system.db :as db]
    [integrated-learning-system.db.sql.commons :refer [path-to-sql]]
    [integrated-learning-system.utils.throwable :refer [exn->map]]
    [next.jdbc.sql :as sql])
  (:import [java.util UUID]))


(defn all-students [db-conn]
  (comment "this fn gonna be redefined by hugsql."))
(defn -students-by-usernames [db-conn {:keys [usernames]}]
  (comment "this fn gonna be redefined by hugsql."))
(defn -students-by-student-ids [db-conn {:keys [student_ids]}]
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

(defn students-by-usernames [db-conn {:as argmap :keys [usernames]}]
  (if (s/valid? (s/coll-of string?)
                usernames)
    (-students-by-usernames db-conn {:usernames usernames})
    (do
      (mulog/log ::students-by-usernames-invalid-args
                 :args argmap)
      (throw (IllegalArgumentException. "usernames is not a string collection.")))))


(defn students-by-student-ids [db-conn {:as argmap, :keys [student-ids]}]
  (if (s/valid? (s/coll-of #(instance? UUID %))
                student-ids)
    (-students-by-student-ids db-conn {:student_ids student-ids})
    (do
      (mulog/log ::students-by-student-ids-invalid-args
                 :args argmap)
      (throw (IllegalArgumentException. "student-ids is not a collection of UUID.")))))
