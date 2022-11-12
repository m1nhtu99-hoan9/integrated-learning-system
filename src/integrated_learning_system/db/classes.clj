(ns integrated-learning-system.db.classes
  (:require [com.brunobonacci.mulog :as mulog]
            [hugsql.core :as hugsql]
            [integrated-learning-system.db :as db]
            [integrated-learning-system.db.courses :as courses-db]
            [integrated-learning-system.db.sql.commons :refer [path-to-sql]]
            [integrated-learning-system.utils.throwable :refer [exn->map]]
            [next.jdbc.sql :as sql])
  (:import (java.util UUID)))


(defn classes-by-course-code [db-conn {:keys [course-code]}]
  (comment "this fn gonna be re-defined by hugsql."))
(defn class-by-id [db-conn {:keys [id]}]
  (comment "this fn gonna be re-defined by hugsql."))
(hugsql/def-db-fns (path-to-sql "classes"))

(defn- -add-class-record! [db-conn
                           course-id
                           {:as class-record, :keys [class-name]}]
  (try
    (let [{:keys [id]} (sql/insert! (db/with-snake-kebab-opts db-conn)
                                    :class
                                    {:id         (UUID/randomUUID)
                                     :course-id  course-id
                                     :class-name class-name})]
      {::db/result (class-by-id db-conn {:id id})})

    (catch Exception exn
      (mulog/log ::failed-add-class-record!
                 :exn (exn->map exn (fn [trace-stack]
                                      (->> trace-stack (take 3) (into []))))
                 :args {:course-id    course-id
                        :class-record class-record}))))

(defn add-class! [db-conn
                  {:as class, :keys [course-code]}]
  (try
    (if-some [{course-id :id} (courses-db/course-by-code db-conn
                                                         {:code course-code})]
      (-add-class-record! db-conn course-id class)
      ; else:
      {::db/error {:course-code (str "No courses of code '" course-code "' found.")}})

    (catch Exception exn
      (mulog/log ::failed-add-class!
                 :exn (exn->map exn (fn [trace-stack]
                                      (->> trace-stack (take 3) (into []))))
                 :class-arg class)
      ; rethrows to let next.jdbc handle rollback of transaction, if any
      (throw exn))))
