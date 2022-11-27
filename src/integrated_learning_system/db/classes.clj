(ns integrated-learning-system.db.classes
  (:require
    [com.brunobonacci.mulog :as mulog]
    [hugsql.core :as hugsql]
    [integrated-learning-system.db :as db]
    [integrated-learning-system.db.courses :as courses-db]
    [integrated-learning-system.db.sql.commons :refer [path-to-sql]]
    [integrated-learning-system.db.timeslots :as timeslots-db]
    [integrated-learning-system.utils.datetime :as dt]
    [integrated-learning-system.utils.throwable :refer [exn->map]]
    [next.jdbc.sql :as sql])
  (:import [java.util UUID]
           [java.time LocalDateTime]))

(defn -all-classes [db-conn]
  (comment "this fn gonna be re-defined by hugsql."))
(defn classes-by-course-code [db-conn {:keys [course-code]}]
  (comment "this fn gonna be re-defined by hugsql."))
(defn -class-by-class-name [db-conn {:keys [class_name]}]
  (comment "this fn gonna be re-defined by hugsql."))
(defn -count-class-periods [db-conn {:keys [class_id]}]
  (comment "this fn gonna be re-defined by hugsql."))
(defn -class-class-periods-within-range [db-conn {:keys [class_name from_date to_date]}]
  (comment "this fn gonna be re-defined by hugsql."))
(defn -class-class-period-at-date-by-class-id [db-conn {:keys [class_id date timeslot_number]}]
  (comment "this fn gonna be re-defined by hugsql."))
(defn -class-class-periods [db-conn {:keys [class_name]}]
  (comment "this fn gonna be re-defined by hugsql."))
(defn -class-teacher-by-class-name [db-conn {:keys [class_name]}]
  (comment "this fn gonna be re-defined by hugsql."))
(defn -class-students-by-class-name [db-conn {:keys [class_name]}]
  (comment "this fn gonna be re-defined by hugsql."))
(defn class-by-id [db-conn {:keys [id]}]
  (comment "this fn gonna be re-defined by hugsql."))
(hugsql/def-db-fns (path-to-sql "classes"))


(defn all-classes [db-conn]
  (for [{:as result, :keys [teacher-first-name teacher-last-name teacher-username]} (-all-classes db-conn)]
    (-> result
        (assoc :teacher {:first-name teacher-first-name
                         :last-name teacher-last-name
                         :username teacher-username})
        (dissoc :teacher-first-name :teacher-last-name :teacher-username))))


(defn class-by-class-name [db-conn {:keys [class-name]}]
  (-class-by-class-name db-conn {:class_name class-name}))

(defn count-class-periods [db-conn {:keys [class-id]}]
  (some->> class-id
           (assoc {} :class_id)
           (-count-class-periods db-conn)
           :count))

(defn class-periods-of-class [db-conn {:keys [class-name to-date from-date]}]
  (let [query-strategy (if (or (nil? from-date) (nil? to-date))
                         :all
                         :range),
        query-fns {:all   -class-class-periods
                   :range -class-class-periods-within-range},
        query-param-maps {:all   {:class_name class-name}
                          :range {:class_name class-name
                                  :to_date    to-date
                                  :from_date  from-date}},
        query-fn (get query-fns query-strategy),
        query-params (get query-param-maps query-strategy)]
    (for [result (query-fn db-conn query-params)]
      (update result :school-date (fn [^LocalDateTime v]
                                    (.toLocalDate v))))))

(defn class-class-period-at-date-by-class-id [db-conn {:keys [class-id date timeslot-number]}]
  (let [school-date (dt/->local-date date)]
    (-class-class-period-at-date-by-class-id db-conn
                                             {:class_id        class-id
                                              :date            school-date
                                              :timeslot_number timeslot-number})))

(defn class-teacher-by-class-name [db-conn {:keys [class-name]}]
  (-class-teacher-by-class-name db-conn {:class_name class-name}))

(defn class-students-by-class-name [db-conn {:keys [class-name]}]
  (-class-students-by-class-name db-conn {:class_name class-name}))

;region add-class!

(defn- -add-class-record! [db-conn
                           course-id
                           {:as class-record, :keys [class-name]}]
  (try
    (let [{:keys [id]} (sql/insert! (db/with-snake-kebab-opts db-conn)
                                    :class
                                    {:id         (UUID/randomUUID)
                                     :course-id  course-id
                                     :class-name class-name})]
      {::db/result (class-by-id db-conn {:id id})})

    (catch Exception exn
      (mulog/log ::failed-add-class-record!
                 :exn (exn->map exn (fn [trace-stack]
                                      (->> trace-stack (take 3) (into []))))
                 :args {:course-id    course-id
                        :class-record class-record})
      ; rethrows to let next.jdbc handle rollback of transaction, if any
      (throw exn))))

(defn add-class! [db-conn
                  {:as class, :keys [course-code class-name]}]
  (try
    (if-some [{course-id :id} (courses-db/course-by-code db-conn
                                                         {:code course-code})]
      (if-some [duplicate (class-by-class-name db-conn class)]
        (do
          (mulog/log ::add-class!-found-duplicate
                     :duplicate duplicate)
          {::db/error {:class-name [(str "Class name '" class-name "' is already used.")]}})
        ; else: ok to process
        (-add-class-record! db-conn course-id class))
      {::db/error {:course-code [(str "No courses of code '" course-code "' found.")]}})

    (catch Exception exn
      (mulog/log ::failed-add-class!
                 :exn (exn->map exn (fn [trace-stack]
                                      (->> trace-stack (take 3) (into []))))
                 :class-arg class)
      ; rethrows to let next.jdbc handle rollback of transaction, if any
      (throw exn))))

;endregion

(defn add-class-periods-for-class! [db-conn {:as payload, :keys [class-name class-periods]}]
  (try
    (let [timeslots (timeslots-db/all-timeslots db-conn),
          timeslot-num-id-map (->> timeslots
                                   (map (fn [t]
                                          [(t :number) (t :id)]))
                                   (into {}))
          {:keys [class-id]} (class-by-class-name db-conn {:class-name class-name}), ; TODO: (when (nil? class-id) ...)
          class-period-rows (for [{:keys [timeslot-num school-date]} class-periods]
                              [class-id (timeslot-num-id-map timeslot-num) school-date])] ; TODO: (when (nil? timeslot-id) ...)
      (sql/insert-multi!
        (db/with-snake-kebab-opts db-conn)
        :class-period
        [:class-id :timeslot-id :school-date]
        class-period-rows))

    (catch Exception exn
      (mulog/log ::failed-add-class-periods-for-class!
                 :exn (exn->map exn (fn [trace-stack]
                                      (->> trace-stack (take 3) (into []))))
                 :payload-arg payload)
      ; rethrows to let next.jdbc handle rollback of transaction, if any
      (throw exn))))
