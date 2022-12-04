(ns integrated-learning-system.views.templates.classes.class-periods
  (:require
    [hiccup.page :refer [html5 include-css include-js]]
    [integrated-learning-system.views.commons
     :as commons
     :refer [common-vendor-js-scripts
             vendor-js-scripts
             vendor-stylesheets]]
    [integrated-learning-system.views.layouts :as layouts]
    [java-time.api :as jt])
  (:use [hiccup.core]))

;;region info page templates

(defn class-period-page [{:keys                  [class-name school-date timeslot-number
                                                  path-errors general-error-message
                                                  having-homework? uris]
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
           [:p.title (str "Class " (h class-name))]
           [:p.subtitle (str (jt/format "dd/MM/uuuu" school-date) " Slot " timeslot-number)]]]
         [:section.container.is-fluid {:id "content-container"}
          [:div.content
           (when having-homework?
             (list
               [:h2.icon-text
                [:span.icon
                 [:i.fas.fa-circle-info]]
                [:span "See:"]]
               [:ul
                [:li [:a {:href (uris :homework)}
                      "Homework"]]]))
           (when-not having-homework?
             (list
               [:h2.icon-text
                [:span.icon
                 [:i.fas.fa-pen-to-square]]
                [:span "Actions:"]]
               [:ul
                [:li [:a {:href (action-uris :add-homework)}
                      "Create New Homework"]]]))]]]))))

(defn class-period-homework-page [{:keys [class-name school-date timeslot-number
                                          path-errors general-error-message
                                          multiple-choice-questions]}]
  (html5
    {:lang "en"}
    (commons/head
      {:title (str (h class-name) " Homework")}
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
           [:p.title (str "Class " (h class-name) ": Homework")]
           [:p.subtitle (str (jt/format "dd/MM/uuuu" school-date) " Slot " timeslot-number)]]]
         [:section.container.is-fluid {:id "content-container"}
          [:div.content
           (for [{:keys [question-title question-options question-number question-slug]} multiple-choice-questions]
             [:div {:data-question-slug   question-slug
                    :data-question-number question-number}
              [:br]
              [:h4 (h question-title)]
              (for [{:keys [option-name option-content is-right]} question-options]
                [:div
                 [:p.icon-text
                  [:span.icon {:style (str "color: var(--" (if is-right "success" "danger") ")")}
                   [:i.fa-regular {:class (if is-right "fa-square-check" "fa-square")}]]
                  [:span
                   [:strong (str option-name ": ")]
                   (h option-content)]]])])]]]))))

;;endregion

;;region action page templates

(defn add-homework-page [{:keys                                            [path-errors general-error-message]
                          {:keys [class-name school-date timeslot-number]} :path-params}]
  (html5
    {:lang "en"}
    (commons/head
      {:title "Add homework"}
      (->> ["bulma-radio"
            "bulma-helpers"]
           (map vendor-stylesheets)
           (apply include-css))
      (->> ["preact"
            "preact/hooks"
            "slugify"]
           (map vendor-js-scripts)
           (concat common-vendor-js-scripts)
           (apply include-js))
      (include-css "/static/stylesheets/homework.css")
      (include-js
        "/static/scripts/layouts.js"
        "/static/scripts/classes/homework/add_homework.js")
      (commons/body
        {:navbar-props nil}
        (cond
          (some? path-errors) (layouts/param-errors-banner path-errors),
          (some? general-error-message) (layouts/errors-banner {:title general-error-message})
          :else
          [:main {:data-class-name      (h class-name)
                  :data-school-date     (jt/format "uuuu-MM-dd" school-date)
                  :data-timeslot-number timeslot-number}
           [:section.hero.is-info {:id "hero-banner"}
            [:div.hero-body
             [:p.title (str (h class-name) " Homework")]
             [:p.subtitle (str (jt/format "dd/MM/uuuu" school-date) " Slot " timeslot-number)]
             [:p.dynamic]]]
           [:div#dynamic-form-group
            [:progress.progress.is-medium.is-info {:max "100"}]]])))))

;;endregion