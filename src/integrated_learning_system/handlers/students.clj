(ns integrated-learning-system.handlers.students
  (:require
    [integrated-learning-system.db.students :as students-db]
    [integrated-learning-system.handlers.commons :refer [user-display-names]]
    [integrated-learning-system.handlers.commons.api :as api]
    [java-time.api :as jt]))

;;-- GET handlers

(defn get-all-students [{{:keys [db-conn]} :services}]
  (cond
    (nil? db-conn) (api/resp-302 "/api/ping")
    ; TODO: pagination
    :else (api/resp-200
            (for [student (students-db/all-students db-conn)]
              (merge (update student :date-of-birth #(some->> % (jt/format "dd/MM/uuuu")))
                     (user-display-names student))))))
