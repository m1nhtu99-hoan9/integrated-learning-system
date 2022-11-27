(ns integrated-learning-system.handlers.webpages.students
  (:require
    [integrated-learning-system.db.students :as students-db]
    [integrated-learning-system.handlers.commons :refer [user-display-names]]
    [integrated-learning-system.handlers.commons.api :as api]
    [integrated-learning-system.handlers.commons.html :refer [resp-200]]
    [integrated-learning-system.views.templates.students :as students-tmpl]))


(defn serve-all-student-pages [{{:keys [db-conn]} :services}]
  (cond
    (nil? db-conn) (api/resp-302 "/api/ping")
    :else (resp-200
            (students-tmpl/all-students-page
              {:students (for [{:as student, :keys [username]} (students-db/all-students db-conn),
                               :let [{:keys [full-name]} (user-display-names student),
                                     timetable-uri (str "/timetable?user-role=student&username=" username)]]
                           (-> student
                               (assoc :full-name full-name)
                               (assoc :uris {:timetable timetable-uri})))}))))
