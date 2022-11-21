(ns integrated-learning-system.handlers.teachers
  (:require
    [integrated-learning-system.db.teachers :as teachers-db]
    [integrated-learning-system.handlers.commons :refer [user-display-names]]
    [integrated-learning-system.handlers.commons.api :as api]))

;;-- GET handlers

(defn get-all-teachers [{{:keys [db-conn]} :services}]
  (cond
    (nil? db-conn) (api/resp-302 "/api/ping")
    ; TODO: pagination
    :else (api/resp-200
            (for [{:as teacher} (teachers-db/all-teachers db-conn)]
              (merge teacher (user-display-names teacher))))))
