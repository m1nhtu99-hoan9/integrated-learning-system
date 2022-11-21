(ns integrated-learning-system.handlers.students
  (:require
    [integrated-learning-system.db.students :as students-db]
    [integrated-learning-system.handlers.commons :refer [user-display-names]]
    [integrated-learning-system.handlers.commons.api :as api]))

;;-- GET handlers

(defn get-all-students [{{:keys [db-conn]} :services}]
  (cond
    (nil? db-conn) (api/resp-302 "/api/ping")
    ; TODO: pagination
    :else (api/resp-200
            (for [{:as student} (students-db/all-students db-conn)]
              (merge student
                     (user-display-names student))))))
