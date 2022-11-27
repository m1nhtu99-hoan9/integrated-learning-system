(ns integrated-learning-system.handlers.timetable.commons
  "Shared functions between API & SSR handler for processing/querying timetable."
  (:require
    [clojure.algo.generic.functor :refer [fmap]]
    [com.brunobonacci.mulog :as mulog]
    [integrated-learning-system.db.students :as students-db]
    [integrated-learning-system.db.teachers :as teachers-db]
    [integrated-learning-system.handlers.commons :refer [user-display-names]]
    [integrated-learning-system.specs :refer [spec-explanation->validation-result]]
    [integrated-learning-system.specs.requests.timetable :as s-timetable]
    [integrated-learning-system.utils.datetime :as dt]
    [integrated-learning-system.utils.throwable :refer [exn->map]]
    [java-time.api :as jt]))


(defn -user-week-ok-timetable [db-conn {:as params, :keys [week-first-date user-role user-display-name user-full-name
                                                           teacher-id student-id]}]
  (try
    (let [week-and-year-fn (fn [date]
                             (jt/as date :aligned-week-of-year :week-based-year)),
          week-last-date (jt/plus week-first-date (jt/days 6)),
          [prev-week-num prev-week-year] (as-> week-first-date $
                                               (jt/minus $ (jt/days 1))
                                               (week-and-year-fn $)),
          [next-week-num next-week-year] (as-> week-last-date $
                                               (jt/plus $ (jt/weeks 1))
                                               (week-and-year-fn $)),
          db-query-params {:student-id student-id
                           :teacher-id teacher-id
                           :from-date  week-first-date
                           :to-date    week-last-date},
          timetable-entries (case user-role
                              :teacher (teachers-db/teacher-timetable-by-teacher-id db-conn db-query-params)
                              :student (students-db/student-timetable-by-student-id db-conn db-query-params)),
          timetable-entries (group-by :school-date
                                      timetable-entries),
          ; would be of pseudo-type IPersistentMap<LocalDate,<IPersistentMap<Integer,ISeq>>>
          timetable-entries (fmap #(group-by :timeslot-number %)
                                  timetable-entries)]
      [200 {:user-display-name user-display-name,
            :user-full-name user-full-name,
            :from-date         week-first-date,
            :to-date           week-last-date,
            :prev-week         {:week prev-week-num
                                :year prev-week-year},
            :next-week         {:week next-week-num
                                :year next-week-year},
            :timetable         timetable-entries}])

    (catch Exception exn
      (mulog/log ::failed-user-week-ok-timetable
                 :args params
                 :exn (exn->map exn (fn [stack]
                                      (->> stack (take 8) (into [])))))
      [500 "Failed to process this request for timetable."])))


(defn user-week-timetable [db-conn coercion-problems {:as params, :keys [year week username user-role]}]
  (try
    (cond
      (some? coercion-problems) [400 (spec-explanation->validation-result s-timetable/validation-messages
                                                                          coercion-problems)],
      (nil? db-conn) [302 "/api/ping"],
      (or (nil? user-role)
          (= user-role :admin)) [302 "/"],
      :else
      (let [{:as teacher, :keys [teacher-id]} (teachers-db/teacher-by-username db-conn {:username username}),
            {:as student, :keys [student-id]} (students-db/student-by-username db-conn {:username username}),
            {teacher-display-name :display-name, teacher-full-name :full-name} (some-> teacher user-display-names),
            {student-display-name :display-name, student-full-name :full-name} (some-> student user-display-names),
            year-date (if (some? year)
                        (jt/local-date year)
                        (jt/local-date)),
            week-first-date (dt/with-week-date year-date
                                               {:week-of-year week, :day-of-week 1})]
        (cond
          (and (= user-role :teacher)
               (nil? teacher)) [422 {(if (some? student)
                                       :user-role
                                       :username) [(str "No teacher with username '" username "' exists in the system.")]}],
          (and (= user-role :student)
               (nil? student)) [422 {(if (some? teacher)
                                       :user-role
                                       :username) [(str "No teacher with username '" username "' exists in the system.")]}],
          :otherwise (-user-week-ok-timetable
                       db-conn
                       {:week-first-date   week-first-date
                        :username          username
                        :user-role         user-role
                        :teacher-id        teacher-id
                        :student-id        student-id
                        :user-display-name (case user-role
                                             :teacher teacher-display-name,
                                             :student student-display-name),
                        :user-full-name    (case user-role
                                             :teacher teacher-full-name
                                             :student student-full-name)}))))
    (catch Exception exn
      (mulog/log ::failed-user-week-timetable
                 :args params
                 :exn (exn->map exn (fn [stack]
                                      (->> stack (take 12) (into [])))))
      [500 "Failed to process this request for timetable."])))
