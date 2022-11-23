(ns integrated-learning-system.handlers.webpages
  (:require
    [integrated-learning-system.handlers.commons.html :refer [resp-200 resp-404]]
    [integrated-learning-system.views.templates :refer [home-page not-found-page]]))


(defn serve-home-page [_]
  (resp-200 (home-page)))

(defn serve-page-404 [_]
  (resp-404 (not-found-page)))
