(ns integrated-learning-system.views.commons
  (:use hiccup.core)
  (:require
    [integrated-learning-system.views.layouts :refer [navbar]]
    [hiccup.page :refer [include-css]]))

(defonce
  ^:private gg-font-stylesheet-url
  (str
    "https://fonts.googleapis.com/css2?family=Bitter:ital,wght@0,400;0,500;0,700;1,500;1,700"
    "&family=Roboto+Serif:opsz,wght@8..144,600&family=Source+Sans+Pro:ital@0;1&display=swap"))

(defn- base-head [{:keys [title]}]
  [:head
   [:meta {:charset "utf-8"}]
   [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
   [:title (h title)]                                       ; hiccup.core/h is for escaping string
   [:link {:rel "preconnect" :href "https://fonts.googleapis.com"}]
   [:link {:rel "preconnect" :href "https://fonts.gstatic.com" :crossorigin "true"}]
   (include-css
     "https://cdn.jsdelivr.net/npm/bulma@0.9.4/css/bulma.min.css"
     "https://cdn.jsdelivr.net/npm/@fortawesome/fontawesome-free@6.2.0/css/all.min.css"
     "https://cdn.jsdelivr.net/npm/docsearch.js@2/dist/cdn/docsearch.min.css")
   [:link {:rel "stylesheet" :href gg-font-stylesheet-url}]
   (include-css "/static/stylesheets/commons.css")])

(defn head [{:as head-props} & elems]
  (html (as-> head-props $
              (base-head $)
              (concat $ elems)
              (into [] $))))

(defn body [{:keys [navbar-props]} & elems]
  (html (->> elems
             (concat [:body.has-navbar-fixed-top
                      (navbar navbar-props)])
             (into []))))
