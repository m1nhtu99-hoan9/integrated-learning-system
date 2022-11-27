(ns integrated-learning-system.handlers.webpages.teachers
  (:require
    [integrated-learning-system.db.teachers :as teachers-db]
    [integrated-learning-system.handlers.commons :refer [user-display-names]]
    [integrated-learning-system.handlers.commons.api :as api]
    [integrated-learning-system.handlers.commons.html :refer [resp-200]]
    [integrated-learning-system.views.templates.teachers :as teachers-tmpl]))


(defn serve-all-teacher-pages [{{:keys [db-conn]} :services}]
  (cond
    (nil? db-conn) (api/resp-302 "/api/ping")
    :else (resp-200
            (teachers-tmpl/all-teachers-page
              {:teachers (for [{:as teacher, :keys [username]} (teachers-db/all-teachers db-conn),
                               :let [{:keys [full-name]} (user-display-names teacher),
                                     timetable-uri (str "/timetable?user-role=teacher&username=" username)]]
                           (-> teacher
                               (assoc :full-name full-name)
                               (assoc :uris {:timetable timetable-uri})))}))))

