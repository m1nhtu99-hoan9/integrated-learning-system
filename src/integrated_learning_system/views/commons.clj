(ns integrated-learning-system.views.commons
  (:use hiccup.core)
  (:require [hiccup.page :refer [include-css]]))

(defn- base-head [{:keys [title]}]
  [:head
   [:meta {:charset "utf-8"}]
   [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
   [:title (h title)]                                       ; hiccup.core/h is for escaping string
   [:link {:rel "preconnect" :href "https://fonts.googleapis.com"}]
   [:link {:rel "preconnect" :href "https://fonts.gstatic.com" :crossorigin "true"}]
   (include-css "/static/stylesheets/commons.css")
   (include-css
     "https://cdn.jsdelivr.net/npm/bulma@0.9.4/css/bulma.min.css"
     "https://bulma.io/vendor/fontawesome-free-5.15.2-web/css/all.min.css"
     "https://cdn.jsdelivr.net/npm/docsearch.js@2/dist/cdn/docsearch.min.css"
     "https://fonts.googleapis.com/css2?family=Bitter:ital,wght@0,400;0,500;0,700;1,500;1,700&family=Source+Sans+Pro:ital@0;1&display=swap")])

(defn head [head-props & elems]
  (html (as-> head-props $
              (base-head $)
              (concat $ elems)
              (into [] $))))
