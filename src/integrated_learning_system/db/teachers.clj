(ns integrated-learning-system.db.teachers
  (:require
    [clojure.spec.alpha :as s]
    [com.brunobonacci.mulog :as mulog]
    [hugsql.core :as hugsql]
    [integrated-learning-system.db :as db]
    [integrated-learning-system.db.sql.commons :refer [path-to-sql]]
    [integrated-learning-system.utils.throwable :refer [exn->map]]
    [next.jdbc.sql :as sql])
  (:import [java.util UUID]
           [java.time LocalDate LocalDateTime]))


(defn all-teachers [db-conn]
  (comment "this fn gonna be redefined by hugsql."))
(defn -teacher-timetable-by-teacher-id [db-conn {:keys [teacher_id from_date to_date]}]
  (comment "this fn gonna be re-defined by hugsql."))
(defn -teacher-by-username [db-conn {:keys [username]}]
  (comment "this fn gonna be redefined by hugsql."))
(defn -teachers-by-teacher-ids [db-conn {:keys [teacher_ids]}]
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

(defn teacher-by-username [db-conn {:keys [username]}]
  ; the param map might contain more keys than just :username
  (-teacher-by-username db-conn {:username username}))

(defn teachers-by-teacher-ids [db-conn {:as argmap, :keys [teacher-ids]}]
  (if (s/valid? (s/coll-of #(instance? UUID %))
                teacher-ids)
    (-teachers-by-teacher-ids db-conn {:teacher_ids teacher-ids})
    (do
      (mulog/log ::teachers-by-teacher-ids-invalid-args
                 :args argmap)
      (throw (IllegalArgumentException. "teacher-ids is not a collection of UUID")))))


(defn teacher-timetable-by-teacher-id [db-conn {:keys [teacher-id, ^LocalDate from-date, ^LocalDate to-date]}]
  (for [result (-teacher-timetable-by-teacher-id db-conn {:teacher_id teacher-id
                                                          :from_date  from-date
                                                          :to_date    to-date})]
    (update result :school-date (fn [^LocalDateTime v]
                                  (some-> v (.toLocalDate))))))
