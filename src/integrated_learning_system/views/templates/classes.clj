(ns integrated-learning-system.views.templates.classes
  (:require
    [hiccup.page :refer [html5 include-css include-js]]
    [java-time.api :as jt]
    [integrated-learning-system.utils.datetime :as dt]
    [integrated-learning-system.utils.json :refer [->json-string]]
    [integrated-learning-system.views.commons :as commons]
    [integrated-learning-system.views.layouts :as layouts]
    [integrated-learning-system.views.templates.classes.commons :refer [common-vendor-js-scripts
                                                                        vendor-js-scripts
                                                                        vendor-stylesheets]])
  (:use [hiccup.core])
  (:import [java.time LocalTime DayOfWeek]))


;;region info page templates

(defn all-classes-page [{:keys [classes]}]
  (html5
    {:lang "en"}
    (commons/head
      {:title "All classes"}
      (include-css "/static/stylesheets/class-info.css"))
    (commons/body
      {:navbar-props nil}
      [:main
       [:section.hero.is-link {:id "hero-banner"}
        [:div.hero-body
         [:p.title "Classes"]]]
       [:section.container.is-fluid {:id "classes-content-container"}
        [:div.content
         [:p]
         [:table.is-fullwidth.is-striped {:class "table"}
          [:tr
           [:th [:span "Class Name"]]
           [:th [:span "Teacher Name (Teacher Username)"]]
           [:th [:span "(Course Code) Course Name"]]
           [:th [:span "Course Description"]]]
          (for [class classes,
                :let [{:keys                                [class-name course-code course-name course-description],
                       {teacher-display-name :display-name} :teacher} class]]
            [:tr
             [:td [:span
                   [:a {:href (str "./" class-name "/")} class-name]]]
             [:td (or teacher-display-name
                      [:em (h "<Not Assigned>")])]  ; `hiccup.core/h` escapes string
             [:td (str "(" course-code ") " course-name)]
             [:td course-description]])]]]])))


(defn class-page [{:keys                  [class-name path-errors general-error-message uris]
                   {action-uris :actions} :uris}]
  (html5
    {:lang "en"}
    (commons/head
      {:title (h class-name)}
      (include-css
        "/static/stylesheets/classes.css"
        "/static/stylesheets/class-info.css"))
    (commons/body
      {:navbar-props nil}
      (cond
        (some? path-errors) (layouts/param-errors-banner path-errors),
        (some? general-error-message) (layouts/errors-banner {:title general-error-message}),
        :else
        [:main {:data-class-name (h class-name)}
         [:section.hero.is-link {:id "hero-banner"}
          [:div.hero-body
           [:p.title (str "Class " (h class-name))]]]
         [:section.container.is-fluid {:id "content-container"}
          [:div.content
           [:h2.icon-text
            [:span.icon
             [:i.fas.fa-circle-info]]
            [:span "See:"]]
           [:ul
            [:li [:a {:href (uris :timetable)}
                  "Class timetable"]]]
           [:h2.icon-text
            [:span.icon
             [:i.fas.fa-pen-to-square]]
            [:span "Actions:"]]
           [:ul
            [:li [:a {:href (action-uris :manage-class-members)}
                  "Manage class members"]]
            [:li [:a {:href (action-uris :organise-weekly-schedule)}
                  "Organise class weekly schedule"]]]]]]))))

;;endregion

;;region action page templates

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

(defn organise-schedule-page [{:as opts, :keys [class-name path-errors class-periods-num]}]
  (html5
    {:lang "en"}
    (commons/head
      {:title (if (nil? class-name)
                "Organise weekly schedule"
                (str "Class " (h class-name) ": Organise weekly schedule"))}
      (include-css
        (vendor-stylesheets "vanillajs-datepicker")
        (vendor-stylesheets "bulma-checkbox"))
      (-> ["vanillajs-datepicker"
           "xstate"
           "xstate-react-fsm"]
          (select-keys vendor-js-scripts)
          (apply include-js))
      (apply include-js
             common-vendor-js-scripts)
      (include-css "/static/stylesheets/classes.css")
      (include-js
        "/static/scripts/layouts.js"
        "/static/scripts/classes/organise_weekly_schedule.js"
        "/static/scripts/classes/fsm/organise_weekly_schedule.js"))
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

(defn manage-class-members-page [{:keys [class-name path-errors]}]
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
      (apply include-js common-vendor-js-scripts)
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

;;endregion

