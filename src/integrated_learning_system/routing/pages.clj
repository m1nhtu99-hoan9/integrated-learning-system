(ns integrated-learning-system.routing.pages
  (:require [integrated-learning-system.handlers.webpages :as h]))

(defn webpage-routes []
  ["" {:no-doc true}
   ["/" h/serve-home-page]
   ["/home" h/serve-home-page]
   ["/404" h/serve-page-404]])
