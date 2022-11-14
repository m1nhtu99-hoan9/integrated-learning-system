(ns integrated-learning-system.handlers.webpages
  (:require
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

(defn serve-timetable-page [{:keys [coercion-problems services],
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
        (resp-200
          (tmpl/timetable-page {:from-date week-first-date,
                                :to-date   (jt/plus week-first-date (jt/days 6))}))))))
