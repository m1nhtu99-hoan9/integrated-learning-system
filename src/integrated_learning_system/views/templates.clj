(ns integrated-learning-system.views.templates
  (:require
    [hiccup.page :refer [html5 include-css]]
    [integrated-learning-system.views.commons :as commons]
    [integrated-learning-system.views.layouts :as layouts]
    [java-time.api :as jt])
  (:use [hiccup.core]))


(defn not-found-page []
  (html5
    {:lang "en"}
    (commons/head {:title "Requested page not found!"}
      (include-css "https://fonts.googleapis.com/css?family=Montserrat:400"
                   "https://fonts.googleapis.com/css?family=Chango")
      (include-css "/static/stylesheets/page404.css"))
    ; Template from https://colorlib.com/wp/template/colorlib-error-404-13/
    (commons/body {:navbar-props {}}
      [:main
       [:section#notfound
        [:div.notfound
         [:div
          [:div.notfound-404
           [:h1 "!"]]
          [:h2 "Error" [:br] "404"]]
         [:p "The page you are looking for might have been removed, had its name
          changed or is temporarily unavailable."
          [:a {:href "/"} " Back to homepage."]]]]])))


(defn home-page []
  (html5
    {:lang "en"}
    (commons/head {:title "Integrated Learning System"})
    (commons/body {:navbar-props {}})))

;region timetable-page

(defn- -timetable-navlinks-level [{:keys [uris]}]
  [:nav.level
   [:div.level-left
    [:p.level-item
     [:a.button.is-primary.is-link.is-light {:href (uris :prev-week)}
      [:span.icon.is-medium [:i.fas.fa-circle-chevron-left]]
      [:span "Previous week"]]]]
   [:div
    [:p.level-item
     [:a.button.is-primary.is-link.is-light {:href "/timetable"}
      [:span "Current week"]]]]
   [:div.level-right
    [:p.level-item
     [:a.button.is-primary.is-link.is-light {:href (uris :next-week)}
      [:span "Next week"]
      [:span.icon.is-medium [:i.fas.fa-circle-chevron-right]]]]]])

(defn timetable-page [{:as opts, :keys [from-date to-date timeslots param-errors uris]}]
  (html5
    {:lang "en"}
    (commons/head {:title "Timetable"})
    (commons/body {:navbar-props {}}
       (if (some? param-errors)
        (layouts/param-errors-banner param-errors)
        ; else:
        (let [from-date-txt (jt/format "dd/MM/uuuu" from-date),
              to-date-txt (jt/format "dd/MM/uuuu" to-date)]
          [:main
           [:section.hero.is-info
            [:div.hero-body
             [:p.title "Timetable"]
             [:p.subtitle (str from-date-txt "-" to-date-txt)]]]
           [:section {:class "section"}
            (-timetable-navlinks-level opts)
            [:div.content
             (layouts/timetable-table opts)]]])))))

;endregion