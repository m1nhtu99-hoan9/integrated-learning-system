(ns integrated-learning-system.handlers.teachers
  (:require
    [integrated-learning-system.db.teachers :as teachers-db]
    [integrated-learning-system.handlers.commons.api :as api]))

;;-- GET handlers

(defn get-all-teachers [{{:keys [db-conn]} :services}]
  (cond
    (nil? db-conn) (api/resp-302 "/api/ping")
    ; TODO: pagination
    :else (api/resp-200
            (for [{:as teacher, :keys [username first-name last-name]} (teachers-db/all-teachers db-conn)
                  :let [full-name (str first-name " " last-name),
                        display-name (str full-name " (" username ")")]]
              (assoc teacher :full-name full-name
                             :display-name display-name)))))
