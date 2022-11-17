(ns integrated-learning-system.handlers.webpages
  (:require
    [integrated-learning-system.db.timeslots :as timeslots-db]
    [integrated-learning-system.handlers.commons.html :refer [resp-200 resp-400 resp-404]]
    [integrated-learning-system.specs :refer [spec-explanation->validation-result]]
    [integrated-learning-system.specs.requests.timetable :as s-timetable]
    [integrated-learning-system.views.templates :as tmpl :refer [home-page not-found-page]]
    [integrated-learning-system.utils.datetime :as dt]
    [java-time.api :as jt]))


(defn serve-home-page [_]
  (resp-200 (home-page)))

(defn serve-page-404 [_]
  (resp-404 (not-found-page)))

;region timetable page(s)

(defn- -serve-timetable-ok-page [db-conn week-first-date]
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
                            (apply build-uri-fn $))]
    (-> {:from-date week-first-date
         :to-date week-last-date
         :uris {:prev-week prev-week-uri
                :next-week next-week-uri}
         :timeslots (timeslots-db/all-timeslots db-conn)}
        tmpl/timetable-page
        resp-200)))

(defn serve-timetable-page [{:keys                        [coercion-problems services],
                             {{:keys [year week]} :query} :parameters}]
  (if (some? coercion-problems)
    (resp-400
      (tmpl/timetable-page {:query-errors (spec-explanation->validation-result s-timetable/validation-messages
                                                                               coercion-problems)}))
    (let [db-conn (:db-conn services),
          year-date (if (some? year)
                      (jt/local-date year)
                      (jt/local-date)),
          week-first-date (dt/with-week-date year-date
                                             {:week-of-year week, :day-of-week 1})]
      (if-some [week-value-error (::dt/error week-first-date)]
        (resp-400
          (tmpl/timetable-page {:query-errors {:week week-value-error}}))
        (-serve-timetable-ok-page db-conn week-first-date)))))

;endregion