(ns integrated-learning-system.views.layouts
  "Shared layout components"
  (:use hiccup.core)
  (:require
    [integrated-learning-system.utils.datetime :as dt]
    [java-time.api :as jt])
  (:import [java.time LocalDate LocalTime]))

;region navbar

(def ^:private div-navbar-more-items
  [:div.navbar-item.has-dropdown.is-hoverable
   [:a.navbar-link "More"]
   [:div.navbar-dropdown
    [:a.navbar-item {:href "/swagger"}
     "Developer API References"]
    [:hr.navbar-divider]
    [:a.navbar-item "About Us"]]])

(def ^:private navbar-burger
  ; hidden for laptop screen, shown in smaller screens
  [:a.navbar-burger {:role        "button"
                     :aria-label  "menu" :aria-expanded "false"
                     :data-target "navbar-menu-contents"}
   [:span {:aria-hidden "true"}]
   [:span {:aria-hidden "true"}]
   [:span {:aria-hidden "true"}]])

(defn navbar [{:as props}]
  [:nav.navbar.is-fixed-top {:role "navigation" :aria-label "main navigation"}
   [:div.navbar-brand
    [:a.navbar-item {:href "/"}
     [:span#brand-title "Integrated Learning System"]]
    navbar-burger]
   [:div.navbar-menu {:id "navbar-menu-contents"}
    [:div.navbar-start
     [:a.navbar-item {:href "/timetable"} "Timetable"]
     [:a.navbar-item "Classes"]
     div-navbar-more-items]
    [:div.navbar-end
     [:div.navbar-item
      [:div.buttons
       [:a.button.is-primary
        [:strong "Sign up"]]
       [:a.button.is-light "Log in"]]]]]])

;endregion

;region banners

(defn errors-banner [{:keys [title errors]}]
  (html
    [:section.hero.is-danger.is-medium
     [:div.hero-body
      [:p.title
       [:span.icon-text
        [:span.icon
         [:i.fas.fa-exclamation-circle]]
        [:span (h title)]]]
      [:div.content
       [:ul
        (for [[field-name error-messages] errors]
          [:li
           [:code (h field-name)]
           [:ul (for [error-message error-messages]
                  [:li error-message])]])]]]]))

(defn param-errors-banner [{:as param-errors}]
  (errors-banner {:title "URL contains invalid parameters:"
                  :errors param-errors}))

;endregion

;region timetable-table

(defn- -timetable-date-heads [{:keys [weekdays]}]
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

(defn- -timetable-slot-rows [{:keys [timeslots dates-count]}]
  (for [{:keys [^Integer number, ^LocalTime start-at, ^Integer duration-mins]} timeslots
        :let [end-at (jt/plus start-at (jt/minutes duration-mins)),
              start-at-txt (dt/local-time->string start-at :time/without-seconds),
              end-at-txt (dt/local-time->string end-at :time/without-seconds)]]
    [:tr {:data-slot-no number
          :data-start-at start-at-txt
          :data-end-at end-at-txt}
     [:th {:scope "row"}
      [:span (str "Slot " number)]
      [:br]
      [:span (str start-at-txt " - " end-at-txt)]]
     (repeat dates-count [:td])]))

(defn timetable-table [{:keys [^LocalDate from-date
                               ^LocalDate to-date
                               timeslots]}]
  (let [dates (into [] (dt/date-range from-date to-date)),
        dates-count (count dates)]
    (html
      [:table.is-fullwidth.is-hoverable {:class "table"}
       [:tr
        [:th [:abbr {:title "Timeslot Number"}
              "Slot No."]]
        (-timetable-date-heads {:weekdays dates})]
       (-timetable-slot-rows {:timeslots timeslots, :dates-count dates-count})])))

;endregion