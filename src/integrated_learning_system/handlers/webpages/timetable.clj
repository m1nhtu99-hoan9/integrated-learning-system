(ns integrated-learning-system.handlers.webpages.timetable
  (:require
    [integrated-learning-system.db.timeslots :as timeslots-db]
    [integrated-learning-system.handlers.commons.api :as api]
    [integrated-learning-system.handlers.commons.html :refer [resp-200 resp-400 resp-422 resp-500]]
    [integrated-learning-system.handlers.timetable.commons :as commons]
    [integrated-learning-system.views.templates.timetable :as tmpl]
    [java-time.api :as jt]))


(defn- -serve-timetable-ok-page [db-conn
                                 {:as page-params, :keys [username user-role user-display-name user-full-name
                                                          year week from-date to-date prev-week next-week]}]
  (let [build-uri-fn (fn [week-num year]
                       (str "/timetable?year=" year "&week=" week-num
                            "&username=" username "&user-role=" (name user-role)))
        prev-week-uri (build-uri-fn (:week prev-week) (:year prev-week)),
        next-week-uri (build-uri-fn (:week next-week) (:year next-week)),
        current-week-uri (build-uri-fn week year)]
    (-> page-params
        (dissoc :prev-week :next-week)
        (assoc :uris {:prev-week prev-week-uri
                      :next-week next-week-uri
                      :current-week current-week-uri}
               :page-title (str user-display-name "'s personal timetable ["
                                (jt/format "dd/MM" from-date) " - " (jt/format "dd/MM" to-date) "]")
               :banner-title (str user-full-name "'s Timetable")
               :timeslots (timeslots-db/all-timeslots db-conn))
        (tmpl/timetable-page)
        (resp-200))))

(defn serve-timetable-page [{:keys                                           [coercion-problems],
                             {:keys [db-conn]}                               :services,
                             ; TODO: get signed-in `username` & `user-role` from session/header instead of query-params
                             {query-params :query} :parameters}]
  (let [[status-code result] (commons/user-week-timetable db-conn coercion-problems query-params)]
    (case status-code
      200 (-serve-timetable-ok-page db-conn (merge query-params result)),
      302 (api/resp-302 result),
      400 (-> {:param-errors result} (tmpl/timetable-page) (resp-400)),
      422 (-> {:param-errors result} (tmpl/timetable-page) (resp-422)),
      500 (-> {:server-error-message result} (tmpl/timetable-page) (resp-500)))))
