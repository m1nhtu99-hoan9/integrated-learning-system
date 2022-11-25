(ns integrated-learning-system.db.homeworks
  (:require
    [hugsql.core :as hugsql]
    [integrated-learning-system.db.sql.commons :refer [path-to-sql]]))


(defn -add-homework! [db-conn {:keys [class_period_id due_class_period_id course_id course_session_id total_score]}]
  (comment "hugsql will re-define this fn."))
(hugsql/def-db-fns (path-to-sql "homeworks"))

(defn add-homework [db-conn {:keys [due-class-period-id course-id course-session-id class-period-id total-score]
                             :or {total-score 100}}]
  (-add-homework! db-conn {:due_class_period_id due-class-period-id
                           :course_id course-id
                           :total_score total-score
                           :course_session_id course-session-id
                           :class_period_id class-period-id}))
