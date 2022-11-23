(ns integrated-learning-system.handlers.webpages.timetable
  (:require
    [clojure.algo.generic.functor :refer [fmap]]
    [integrated-learning-system.db.students :as students-db]
    [integrated-learning-system.db.teachers :as teachers-db]
    [integrated-learning-system.db.timeslots :as timeslots-db]
    [integrated-learning-system.handlers.commons.api :as api]
    [integrated-learning-system.handlers.commons.html :refer [resp-200 resp-400 resp-422 resp-500]]
    [integrated-learning-system.handlers.timetable.commons :as commons]
    [integrated-learning-system.specs :refer [spec-explanation->validation-result]]
    [integrated-learning-system.specs.requests.timetable :as s-timetable]
    [integrated-learning-system.views.templates.timetable :as tmpl]
    [integrated-learning-system.utils.datetime :as dt]
    [java-time.api :as jt]))


(defn- -serve-timetable-ok-page [db-conn
                                 {:as page-params, :keys [year week username user-role
                                                          from-date to-date prev-week next-week user-display-name]}]
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


(comment
  "Old code. To be removed..."

  (defn serve-timetable-page [{:keys                                           [coercion-problems],
                               {:keys [db-conn]}                               :services,
                               ; TODO: get signed-in `username` & `user-role` from session/header instead of query-params
                               {{:keys [year week username user-role]} :query} :parameters}]
    (cond
      (some? coercion-problems) (resp-400
                                  (tmpl/timetable-page
                                    {:param-errors (spec-explanation->validation-result s-timetable/validation-messages
                                                                                        coercion-problems)})),
      (nil? db-conn) (api/resp-302 "/api/ping"),
      (or (nil? user-role)
          (= user-role :admin)) (api/resp-302 "/"),
      :else
      (let [{:as teacher, :keys [teacher-id]} (teachers-db/teacher-by-username db-conn {:username username}),
            {:as student, :keys [student-id]} (students-db/student-by-username db-conn {:username username}),
            year-date (if (some? year)
                        (jt/local-date year)
                        (jt/local-date)),
            week-first-date (dt/with-week-date year-date
                                               {:week-of-year week, :day-of-week 1})]
        (cond
          (and (= user-role :teacher)
               (nil? teacher)) (resp-422
                                 (tmpl/timetable-page
                                   {:param-errors
                                    {(if (some? student)
                                       :user-role
                                       :username) [(str "No teacher with username '" username "' exists in the system.")]}})),
          (and (= user-role :student)
               (nil? student)) (resp-422
                                 (tmpl/timetable-page
                                   {:param-errors
                                    {(if (some? teacher)
                                       :user-role
                                       :username) [(str "No teacher with username '" username "' exists in the system.")]}})),
          :otherwise (-serve-timetable-ok-page
                       db-conn
                       {:week-first-date week-first-date
                        :username        username
                        :user-role       user-role
                        :teacher-id      teacher-id
                        :student-id      student-id})))))

  (defn- -serve-timetable-ok-page [db-conn {:keys [week-first-date user-role username teacher-id student-id]}]
    (let [week-and-year-fn (fn [date]
                             (jt/as date :aligned-week-of-year :week-based-year)),
          build-uri-fn (fn [week-num year]
                         (str "/timetable?year=" year "&week=" week-num)),
          week-last-date (jt/plus week-first-date (jt/days 6)),
          prev-week-uri (as-> week-first-date $
                              (jt/minus $ (jt/days 1))
                              (week-and-year-fn $)
                              (apply build-uri-fn $)),
          next-week-uri (as-> week-last-date $
                              (jt/plus $ (jt/weeks 1))
                              (week-and-year-fn $)
                              (apply build-uri-fn $)),
          db-query-params {:student-id student-id
                           :teacher-id teacher-id
                           :from-date  week-first-date
                           :to-date    week-last-date},
          timeslots (case user-role
                      :teacher (teachers-db/teacher-timetable-by-teacher-id db-conn db-query-params)
                      :student (students-db/student-timetable-by-student-id db-conn db-query-params)),
          timeslots (group-by :school-date
                              timeslots),
          timeslots (fmap #(group-by :timeslot-number %)
                          timeslots)]
      (-> {:from-date week-first-date
           :to-date   week-last-date
           :uris      {:prev-week prev-week-uri
                       :next-week next-week-uri}
           :timeslots (timeslots-db/all-timeslots db-conn)}  ; all-timeslots != timeslots
          tmpl/timetable-page
          resp-200))))
