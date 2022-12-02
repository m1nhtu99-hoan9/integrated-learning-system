(ns integrated-learning-system.handlers.webpages.classes
  (:require
    [clojure.algo.generic.functor :refer [fmap]]
    [integrated-learning-system.db.classes :as classes-db]
    [integrated-learning-system.db.multiple-choice-questions :as mcq-db]
    [integrated-learning-system.db.homeworks :as homework-db]
    [integrated-learning-system.db.timeslots :as timeslots-db]
    [integrated-learning-system.handlers.classes.homework.commons :as homework-commons]
    [integrated-learning-system.handlers.commons :refer [user-display-names]]
    [integrated-learning-system.handlers.commons.api :as api]
    [integrated-learning-system.handlers.commons.html :refer [resp-200 resp-400 resp-422 resp-500 resp-501]]
    [integrated-learning-system.specs :refer [spec-explanation->validation-result]]
    [integrated-learning-system.specs.requests.classes :as s-classes]
    [integrated-learning-system.utils.datetime :as dt]
    [integrated-learning-system.views.templates.classes :as classes-tmpl]
    [integrated-learning-system.views.templates.classes.class-periods :as cp-tmpl]
    [integrated-learning-system.views.templates.timetable :as timetable-tmpl]
    [java-time.api :as jt])
  (:import (java.util Date)))


(defn- -preprocess-class-page [coercion-problems db-conn {:keys [class-name]}]
  (cond
    (some? coercion-problems) [400 {:path-errors (spec-explanation->validation-result s-classes/validation-messages
                                                                                      coercion-problems)}],
    (nil? db-conn) [302 "/api/ping"],
    :else (if-some [this-class (classes-db/class-by-class-name db-conn {:class-name class-name})]
            [200 this-class]
            [400 {:path-errors {:class-name [(str "Class with name '" class-name "' did not exist.")]}}])))


(defn- -preprocess-class-period-page [coercion-problems db-conn {:keys [class-name, ^Date date, slot-no]}]
  (cond
    (some? coercion-problems) [400 {:path-errors (spec-explanation->validation-result s-classes/validation-messages
                                                                                      coercion-problems)}],
    (nil? db-conn) [302 "/api/ping"],
    :else
    (let [school-date (dt/->local-date date),
          path-params {:class-name      class-name
                       :school-date     school-date
                       :timeslot-number slot-no},
          [status-code result] (homework-commons/identify-class-period db-conn path-params)]
      [status-code (case status-code
                     200 {:path-params path-params
                          :entity-ids  result},
                     422 (let [{:keys [title errors]} result]
                           (if (empty? errors)
                             {:general-error-message title}
                             {:path-errors errors})),
                     500 {:general-error-message (str "Failed to load data needed for the page to work."
                                                      "If reloading doesn't fix the issue, please contact your admin.")})])))

;;region info pages

(defn serve-all-classes-page [{{:keys [db-conn]} :services}]
  (if (nil? db-conn)
    (api/resp-302 "/api/ping")
    ; else: happy case
    (as-> (classes-db/all-classes db-conn) $
          (for [entry $]
            (update entry :teacher #(some-> % (as-> teacher
                                                    (merge teacher
                                                           (user-display-names teacher))))))
          (hash-map :classes $)
          (classes-tmpl/all-classes-page $)
          (resp-200 $))))


(defn serve-class-info-page [{:keys                       [coercion-problems],
                              {:keys [db-conn]}           :services,
                              {{:keys [class-name]
                                :as   path-params} :path} :parameters}]
  (let [[code result] (-preprocess-class-page coercion-problems db-conn path-params)]
    (case code
      400 (resp-400 (classes-tmpl/class-page result)),
      301 (api/resp-302 result),
      200 (-> {:class-name class-name
               :uris       {:timetable "./timetable"
                            :actions   {:manage-class-members     "./_actions/manage-class-members"
                                        :organise-weekly-schedule "./_actions/organise-weekly-schedule"}}}
              classes-tmpl/class-page
              resp-200))))


(defn serve-class-timetable-page [{:keys                                          [coercion-problems],
                                   {:keys [db-conn]}                              :services,
                                   {{:as path-params, :keys [class-name]} :path,
                                    {:keys [year week]}                   :query} :parameters}]
  (let [[code result] (-preprocess-class-page coercion-problems db-conn path-params)]
    (case code
      400 (-> {:param-errors (result :path-errors)}
              timetable-tmpl/timetable-page
              resp-400),
      301 (api/resp-302 result),
      200 (let [{:keys [course-code course-name]} result,   ; `result` is this class
                week-and-year-fn (fn [date]
                                   (jt/as date :aligned-week-of-year :week-based-year)),
                build-uri-fn (fn [week-num year]
                               (str "/classes/" class-name "/timetable?year=" year "&week=" week-num))
                year-date (if (some? year)
                            (jt/local-date year)
                            (jt/local-date)),
                week-first-date (dt/with-week-date year-date
                                                   {:week-of-year week, :day-of-week 1}),
                week-last-date (jt/plus week-first-date (jt/days 6)),
                prev-week-uri (as-> week-first-date $
                                    (jt/minus $ (jt/days 1))
                                    (week-and-year-fn $)
                                    (apply build-uri-fn $)),
                next-week-uri (as-> week-last-date $
                                    (jt/plus $ (jt/weeks 1))
                                    (week-and-year-fn $)
                                    (apply build-uri-fn $)),
                current-week-uri (->> week-last-date
                                      (week-and-year-fn)
                                      (apply build-uri-fn))
                timetable-entries (classes-db/class-periods-of-class db-conn
                                                                     {:class-name class-name
                                                                      :from-date  week-first-date
                                                                      :to-date    week-last-date}),
                timetable-entries (for [entry timetable-entries]
                                    (assoc entry :course-code course-code
                                                 :course-name course-name)),
                timetable-entries (group-by :school-date
                                            timetable-entries),
                ; would be of pseudo-type IPersistentMap<LocalDate,<IPersistentMap<Integer,ISeq>>>
                timetable-entries (fmap #(group-by :timeslot-number %)
                                        timetable-entries)]
            (-> {:page-title   (str class-name " Timetable ["
                                    (jt/format "dd/MM" week-first-date) " - " (jt/format "dd/MM" week-last-date) "]")
                 :banner-title (str class-name " Timetable")
                 :from-date    week-first-date
                 :to-date      week-last-date
                 :timetable    timetable-entries
                 :timeslots    (timeslots-db/all-timeslots db-conn)
                 :uris         {:prev-week    prev-week-uri
                                :next-week    next-week-uri
                                :current-week current-week-uri}}
                (timetable-tmpl/timetable-page)
                (resp-200))))))


(defn serve-class-period-info-page [{:keys             [coercion-problems parameters],
                                     {:keys [db-conn]} :services}]
  (let [[code result] (-preprocess-class-period-page coercion-problems db-conn (:path parameters))]
    (case code
      500 (-> result (cp-tmpl/class-period-page) (resp-500)),
      422 (-> result (cp-tmpl/class-period-page) (resp-422)),
      400 (-> result (cp-tmpl/class-period-page) (resp-400)),
      302 (api/resp-302 result),
      200 (let [{{:keys [class-period-id]} :entity-ids,
                 :keys                     [path-params]} result,
                having-homework? (->> {:due-class-period-id class-period-id}
                                      (homework-db/count-homeworks-by-due-class-period-id db-conn)
                                      (< 0))]
            (->> {:having-homework? having-homework?
                  :uris             {:homework "./homework/"
                                     :actions  {:add-homework "./homework/_actions/add"}}}
                 (merge path-params)
                 (cp-tmpl/class-period-page)
                 (resp-200))))))


(defn serve-class-homework-page [{:keys             [coercion-problems parameters],
                                  {:keys [db-conn]} :services}]
  (let [[code result] (-preprocess-class-period-page coercion-problems db-conn (:path parameters))]
    (case code
      500 (-> result (cp-tmpl/class-period-homework-page) (resp-500)),
      422 (-> result (cp-tmpl/class-period-homework-page) (resp-422)),
      400 (-> result (cp-tmpl/class-period-homework-page) (resp-400)),
      302 (api/resp-302 result),
      200 (let [{{:keys [class-period-id]} :entity-ids,
                 :keys                     [path-params]} result,
                multiple-choice-questions (->> {:due-class-period-id class-period-id}
                                               (mcq-db/multiple-choice-questions-by-due-class-period-id db-conn)),
                ; CAUTION: coll items not containing :question-number key might be first ones in the sorted seq
                multiple-choice-questions (sort-by :question-number multiple-choice-questions)]
            (->> {:multiple-choice-questions multiple-choice-questions}
                 (merge path-params)
                 (cp-tmpl/class-period-homework-page)
                 (resp-200))))))

;;endregion

;;region action pages

(defn serve-organise-schedule-page [{:keys                       [coercion-problems],
                                     {:keys [db-conn]}           :services,
                                     {{:keys [class-name]
                                       :as   path-params} :path} :parameters}]
  (let [[code result] (-preprocess-class-page coercion-problems db-conn path-params)]
    (case code
      400 (resp-400 (classes-tmpl/organise-schedule-page result)),
      301 (api/resp-302 result),
      200 (let [{:keys [class-id]} result,
                class-periods-num (classes-db/count-class-periods db-conn
                                                                  {:class-id class-id})]
            (if (pos-int? class-periods-num)
              ; updating class schedule is not yet supported
              (resp-501 (classes-tmpl/organise-schedule-page {:class-name        class-name
                                                              :class-periods-num class-periods-num}))
              ; else: happy case
              (resp-200
                (classes-tmpl/organise-schedule-page
                  {:class-name class-name
                   :timeslots  (timeslots-db/all-timeslots db-conn)})))))))


(defn serve-manage-class-members-page [{:keys                       [coercion-problems]
                                        {:keys [db-conn]}           :services
                                        {{:keys [class-name]
                                          :as   path-params} :path} :parameters}]
  (let [[code result] (-preprocess-class-page coercion-problems db-conn path-params)]
    (case code
      400 (resp-400 (classes-tmpl/manage-class-members-page result)),
      301 (api/resp-302 result),
      200 (resp-200 (classes-tmpl/manage-class-members-page {:class-name class-name})))))


(defn serve-manage-homework-page [{:keys             [coercion-problems parameters],
                                   {:keys [db-conn]} :services}]

  (let [[status-code result] (-preprocess-class-period-page coercion-problems db-conn (:path parameters))]
    (case status-code
      500 (-> result (cp-tmpl/add-homework-page) (resp-500)),
      422 (-> result (cp-tmpl/add-homework-page) (resp-422)),
      400 (-> result (cp-tmpl/add-homework-page) (resp-400)),
      302 (api/resp-302 result),
      200 (-> result (dissoc :entity-ids) (cp-tmpl/add-homework-page) (resp-200)))))

;;endregion