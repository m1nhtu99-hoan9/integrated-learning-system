(ns integrated-learning-system.views.templates
  (:require
    [hiccup.page :refer [html5 include-css]]
    [integrated-learning-system.views.commons :as commons])

  (:use [hiccup.core]))


(defn not-found-page []
  (html5
    {:lang "en"}
    (commons/head {:title "Requested page not found!"}
                  (include-css "https://fonts.googleapis.com/css?family=Montserrat:400"
                               "https://fonts.googleapis.com/css?family=Chango")
                  (include-css "/static/stylesheets/page404.css"))
    ; Template from https://colorlib.com/wp/template/colorlib-error-404-13/
    (commons/body
      {:navbar-props {}}
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
