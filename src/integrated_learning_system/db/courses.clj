(ns integrated-learning-system.db.courses
  (:require
    [com.brunobonacci.mulog :as mulog]
    [hugsql.core :as hugsql]
    [integrated-learning-system.db :as db]
    [integrated-learning-system.db.sql.commons :refer [path-to-sql]]
    [integrated-learning-system.utils.throwable :refer [exn->map]]
    [next.jdbc.sql :as sql])
  (:import [java.util UUID]
           [org.apache.commons.lang3 NotImplementedException]))


(defn course-by-code [db-conn {:keys [code]}]
  (comment "this fn gonna be redefined by hugsql."))
(defn -courses-by-filters [db-conn {:keys [code-pattern course-name-pattern]}]
  (comment "this fn gonna be redefined by hugsql."))
(hugsql/def-db-fns (path-to-sql "courses"))


(defn add-course! [db-conn
                   {:as course, :keys [course-code course-name description]}]
  (try
    (if (some? (course-by-code db-conn {:code course-code}))
      {::db/error {:username (str "Course with code [" course-code "] already exists.")}}
      {::db/result (sql/insert! (db/with-snake-kebab-opts db-conn)
                                :course
                                {:id          (UUID/randomUUID)
                                 :code        course-code
                                 :course-name course-name
                                 :description description
                                 :status      "ACTIVE"})})

    (catch Exception exn
      (mulog/log ::failed-add-course!
                 :exn (exn->map exn (fn [trace-stack]
                                      (->> trace-stack (take 3) (into []))))
                 :course-arg course)
      ; rethrows to let next.jdbc handle rollback of transaction, if any
      (throw exn))))

(defn courses-by-filters [db-conn {:keys [code course-name]}]
  (-courses-by-filters (db/with-snake-kebab-opts db-conn)
                       {:code-pattern        (db/patternise-for-like code)
                        :course-name-pattern (db/patternise-for-like course-name)}))
