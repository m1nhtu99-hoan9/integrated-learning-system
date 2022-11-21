(ns integrated-learning-system.views.templates.classes
  (:require
    [hiccup.page :refer [html5 include-css include-js]]
    [java-time.api :as jt]
    [integrated-learning-system.utils.datetime :as dt]
    [integrated-learning-system.utils.json :refer [->json-string]]
    [integrated-learning-system.utils.string :refer [utf8-string]]
    [integrated-learning-system.views.commons :as commons]
    [integrated-learning-system.views.layouts :as layouts])
  (:use [hiccup.core])
  (:import [java.time LocalTime DayOfWeek]))


;region organise-weekly-schedule

(defn- -describe-day-of-week [^DayOfWeek weekday]
  (let [day-of-week-num (.getValue weekday),
        day-of-week-abbr (dt/day-of-week-num->string day-of-week-num {:style :text-style/short}),
        day-of-week-full (dt/day-of-week-num->string day-of-week-num {:style :text-style/full})]
    {:day-of-week-num day-of-week-num, :day-of-week-abbr day-of-week-abbr, :day-of-week-full day-of-week-full}))

(defn- -table-date-heads [{:keys [weekdays]}]
  (for [weekday weekdays
        :let [{:keys [day-of-week-num
                      day-of-week-abbr
                      day-of-week-full]} (-describe-day-of-week weekday)]]
    [:th {:data-day-of-week     day-of-week-full
          :data-day-of-week-num day-of-week-num
          :scope                "col"}
     [:span [:abbr {:title day-of-week-full}
             day-of-week-abbr]]]))

(defn- -table-slot-rows [{:keys [timeslots weekdays]}]
  (for [{:keys [^Integer number, ^LocalTime start-at, ^Integer duration-mins]} timeslots
        :let [end-at (jt/plus start-at (jt/minutes duration-mins)),
              start-at-txt (dt/local-time->string start-at :time/without-seconds),
              end-at-txt (dt/local-time->string end-at :time/without-seconds),
              row-metadata {:data-slot-no  number
                            :data-start-at start-at-txt
                            :data-end-at   end-at-txt}]]
    [:tr row-metadata
     [:th {:scope "row"}
      [:span (str "Slot " number)]
      [:br]
      [:span (str start-at-txt " - " end-at-txt)]]
     (for [^DayOfWeek weekday weekdays
           :let [{:keys [day-of-week-num day-of-week-full]} (-describe-day-of-week weekday)]]
       [:td (assoc row-metadata
              :data-day-of-week day-of-week-full
              :data-day-of-week-num day-of-week-num)
        [:div.field
         [:label.b-checkbox.checkbox
          [:input {:type "checkbox" :value "0"}]
          [:span.check.is-info]]]])]))

(defn- -date-range-input-section []
  [:section.container.is-fluid.columns {:id "date-range-input-container"}
   [:div.column.field.is-horizontal
    [:div.field-label.is-normal
     [:label {:class "label"} "Start date:"]]
    [:div.field-body
     [:div.field
      [:p.control
       [:input {:id          "start-date-txt"
                :name        "class-start-date"
                :class       "input" :type "text"
                :placeholder "From which date?"}]]]]]
   [:div.column.field.is-horizontal
    [:div.field-label.is-normal
     [:label {:class "label"} "End date:"]]
    [:div.field-body
     [:div.field
      [:p.control
       [:input {:id          "end-date-txt"
                :name        "class-finish-date"
                :class       "input" :type "text"
                :placeholder "To which date (inclusively)?"}]]]]]])

(defn- -schedule-selection-tables [{:keys [timeslots]}]
  (let [weekdays (apply vector (DayOfWeek/values))]
    [:table.is-fullwidth.is-hoverable {:class "table"}
     [:tr
      [:th [:abbr {:title "Timeslot Number"}
            "Slot No."]]
      (-table-date-heads {:weekdays weekdays})]
     (-table-slot-rows {:timeslots timeslots, :weekdays weekdays})]))

(defn- -class-periods-existed-banner [{:keys [class-name class-periods-num]}]
  (html
    [:section.hero.is-warning.is-medium
     [:div.hero-body
      [:p.title
       [:span.icon-text
        [:span.icon
         [:i.fas.fa-triangle-exclamation]]
        [:span "Updating class schedule is not supported here."]]]
      [:p.subtitle
       [:span (str "Class " (h class-name) " is currently having " class-periods-num " class period(s).")]]]]))


(defn organise-schedule [{:as opts, :keys [class-name path-errors class-periods-num]}]
  (html5
    {:lang "en"}
    (commons/head
      {:title (if (nil? class-name)
                "Organise weekly schedule"
                (str "Class " (h class-name) ": Organise weekly schedule"))}
      (include-css
        "https://cdn.jsdelivr.net/npm/vanillajs-datepicker@1.2.0/dist/css/datepicker.min.css"
        "https://cdn.jsdelivr.net/npm/bulma-checkbox@1.2.1/css/main.min.css")
      (include-js
        "https://cdn.jsdelivr.net/npm/vanillajs-datepicker@1.2.0/dist/js/datepicker-full.min.js"
        "https://cdn.jsdelivr.net/npm/ramda@0.28.0/dist/ramda.min.js"
        "https://cdn.jsdelivr.net/npm/htm@3.1.1/dist/htm.js"
        "https://cdn.jsdelivr.net/npm/preact@10.11.3/dist/preact.min.js")
      (include-css "/static/stylesheets/classes.css")
      (include-js
        "/static/scripts/layouts.js"
        "/static/scripts/classes/organise_weekly_schedule.js"))
    (commons/body
      {:navbar-props nil}
      (cond
        (some? path-errors) (layouts/param-errors-banner path-errors),
        (pos-int? class-periods-num) (-class-periods-existed-banner opts),
        :else
        [:main {:data-class-name  (h class-name)
                :data-post-v1-uri (str "/api/v1/classes/" (h class-name) "/periods/batch")}
         [:section.hero.is-info {:id "hero-banner"}
          [:div.hero-body
           [:p.title "Organise schedule"]
           [:p.subtitle (str "Class " (h class-name))]
           [:p.dynamic]]]
         (-date-range-input-section)
         [:section.container.is-fluid {:id "selection-table-container"}
          [:div.content
           (-schedule-selection-tables opts)]]
         [:section.container.is-fluid {:id "button-container"}
          [:div.level
           [:div.level-left]
           [:div.level-right
            [:p.level-item
             [:button.is-primary {:class "button"
                                  :id    "submit-btn"}
              [:span.icon.is-small [:i.fas.fa-check]]
              [:span [:strong "Submit"]]]]
            [:p.level-item
             [:button.is-danger.is-outlined {:class "button"
                                             :id    "reset-btn"}
              [:span "Reset"]
              [:span.icon.is-small [:i.fas.fa-times]]]]]]]]))))

;endregion

(defn manage-class-members [{:as opts, :keys [class-name path-errors]}]
  (html5
    {:lang "en"}
    (commons/head
      {:title (if (nil? class-name)
                "Manage class members"
                (str "Class " (h class-name) ": Manage class members"))}
      (comment
        ; unbundled modules imported only when debugging
        [:script {:type "importmap"}
         (->json-string {:imports {"preact" "https://cdn.jsdelivr.net/npm/preact@10.11.3"}})])
      (include-css
        "https://cdn.jsdelivr.net/npm/@creativebulma/bulma-tagsinput@1.0.3/dist/css/bulma-tagsinput.min.css")
      (include-js
        "https://cdn.jsdelivr.net/npm/@creativebulma/bulma-tagsinput@1.0.3/dist/js/bulma-tagsinput.min.js"
        "https://cdn.jsdelivr.net/npm/ramda@0.28.0/dist/ramda.min.js"
        "https://cdn.jsdelivr.net/npm/htm@3.1.1/dist/htm.js"
        "https://cdn.jsdelivr.net/npm/preact@10.11.3/dist/preact.min.js"
        "https://cdn.jsdelivr.net/npm/preact@10.11.3/hooks/dist/hooks.umd.js")
      (include-css "/static/stylesheets/classes.css")
      (include-js
        "/static/scripts/layouts.js"
        "/static/scripts/classes/organise_class_members.js"))
    (commons/body
      {:navbar-props nil}
      (cond
        (some? path-errors) (layouts/param-errors-banner path-errors),
        :else
        [:main {:data-class-name (h class-name)}
         [:section.hero.is-info {:id "hero-banner"}
          [:div.hero-body
           [:p.title "Manage class members"]
           [:p.subtitle (str "Class " (h class-name))]
           [:p.dynamic]]]
         [:div#dynamic-form-group
          [:progress.progress.is-medium.is-info {:max "100"}]]]))))
