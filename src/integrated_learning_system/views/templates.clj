(ns integrated-learning-system.views.templates
  (:require
    [hiccup.page :refer [html5 include-css]]
    [integrated-learning-system.views.commons :as commons]
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
      [:section#notfound
       [:div.notfound
        [:div
         [:div.notfound-404
          [:h1 "!"]]
         [:h2 "Error" [:br] "404"]]
        [:p "The page you are looking for might have been removed, had its name
          changed or is temporarily unavailable."
         [:a {:href "/"} " Back to homepage."]]]])))

(defn home-page []
  (html5
    {:lang "en"}
    (commons/head {:title "Integrated Learning System"})
    (commons/body {:navbar-props {}})))

(defn timetable-page [{:keys [from-date to-date query-errors]}]
  (html5
    {:lang "en"}
    (commons/head {:title "Timetable"})
    (commons/body
      {:navbar-props {}}
      [:span (some->> from-date (jt/format "dd/MM/uuuu") h)] ; hiccup.core/h is function for escaping string
      [:br]
      [:span (some->> to-date (jt/format "dd/MM/uuuu") h)]
      [:br]
      [:span (-> query-errors str h)])))
