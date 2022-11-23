(ns integrated-learning-system.views.templates.timetable
  (:use hiccup.core)
  (:require
    [hiccup.page :refer [html5 include-css]]
    [integrated-learning-system.views.layouts :as layouts]
    [integrated-learning-system.views.commons :as commons]
    [integrated-learning-system.utils.datetime :as dt]
    [java-time.api :as jt])
  (:import [java.time LocalDate LocalTime]))

;;region private layout components

;region timetable table

(defn- -timetable-date-headers [{:keys [weekdays]}]
  (for [date weekdays
        :let [day-of-week-num (jt/as date :day-of-week),
              day-of-week-abbr (dt/day-of-week-num->string day-of-week-num {:style :text-style/short}),
              day-of-week-full (dt/day-of-week-num->string day-of-week-num {:style :text-style/full})]]
    [:th {:data-day-of-week day-of-week-full
          :data-date        (jt/format "dd/MM/uuuu" date)
          :scope            "col"
          :style            "text-align: center;"}
     [:span day-of-week-abbr]
     [:br]
     [:span (jt/format "dd/MM" date)]]))

(defn- -timetable-rows [{:keys [timeslots dates timetable]}]
  ; ASSUMPTIONS:
  ;  - `timeslots` are already sorted by `:number` value
  ;  - `dates` are already sorted
  (for [{^Integer timeslot-number :number, :keys [^LocalTime start-at, ^Integer duration-mins]} timeslots
        :let [end-at (jt/plus start-at (jt/minutes duration-mins)),
              start-at-txt (dt/local-time->string start-at :time/without-seconds),
              end-at-txt (dt/local-time->string end-at :time/without-seconds)]]
    [:tr {:data-slot-no  timeslot-number
          :data-start-at start-at-txt
          :data-end-at   end-at-txt}
     [:th {:scope "row"}
      [:span (str "Slot " timeslot-number)]
      [:br]
      [:span (str start-at-txt " - " end-at-txt)]]
     (for [date dates
           :let [timetable-entries (get-in timetable [date timeslot-number])]]
       [:td
        (for [{:keys [class-name course-code course-name]} timetable-entries]
          [:div
           [:div.divider.is-left (h class-name)]
           [:span [:abbr {:title (h course-name)} (str "(" (h course-code) ")")]]])])]))

(defn- -timetable-table
  "Renders timetable with dates from `from-date` to `to-date` as columns and `timeslots` as rows"
  [{:keys [^LocalDate from-date
           ^LocalDate to-date
           timeslots
           timetable]}]
  (let [dates (into [] (dt/date-range from-date to-date))]
    (html
      [:table.is-fullwidth.is-hoverable.is-bordered {:class "table"}
       (let [date-headers (-timetable-date-headers {:weekdays dates})]
         ; CAVEAT: if vector is used here, hiccup would mistake it for HTML tag
         (list
           [:thead
            [:tr
             [:th [:abbr {:title "Timeslot Number"}
                   "Slot No."]]
             date-headers]]
           [:tfoot
            [:tr
             [:th]  ; intentionally left empty
             date-headers]]))
       (-timetable-rows {:timeslots timeslots, :dates dates, :timetable timetable})])))

;endregion

(defn- -timetable-navlinks-level [{:keys [uris]}]
  [:nav.level
   [:div.level-left
    [:p.level-item
     [:a.button.is-primary.is-link.is-light {:href (uris :prev-week)}
      [:span.icon.is-medium [:i.fas.fa-circle-chevron-left]]
      [:span "Previous week"]]]]
   [:div
    [:p.level-item
     [:a.button.is-primary.is-link.is-light {:href (uris :current-week)}
      [:span "Current week"]]]]
   [:div.level-right
    [:p.level-item
     [:a.button.is-primary.is-link.is-light {:href (uris :next-week)}
      [:span "Next week"]
      [:span.icon.is-medium [:i.fas.fa-circle-chevron-right]]]]]])

;;endregion

;;region pages

(defn timetable-page [{:as page-params, :keys [page-title,
                                               from-date, to-date, timeslots, timetable,
                                               server-error-message, param-errors, uris]}]
  (html5
    {:lang "en"}
    (commons/head
      ; hiccup.core/h is for escaping string
      {:title (if (some? server-error-message)
                server-error-message
                page-title)}
      (include-css "https://cdn.jsdelivr.net/npm/@creativebulma/bulma-divider@1.1.0/dist/bulma-divider.min.css")
      (include-css "/static/stylesheets/timetable.css"))
    (commons/body
      {:navbar-props {}}
      (cond
        (some? param-errors) (layouts/param-errors-banner param-errors),
        (some? server-error-message) (layouts/errors-banner
                                       {:title (str "Server failed to process this request."
                                                    "If reloading doesn't fix the issue, please contact your admin.")}),
        :otherwise
        (let [from-date-txt (jt/format "dd/MM/uuuu" from-date),
              to-date-txt (jt/format "dd/MM/uuuu" to-date)]
          [:main
           [:section.hero.is-info
            [:div.hero-body
             [:p.title "Timetable"]
             [:p.subtitle (str from-date-txt "-" to-date-txt)]]]
           [:section {:class "section"}
            (-timetable-navlinks-level page-params)
            [:div.content
             (-timetable-table page-params)]]])))))

;;endregion
