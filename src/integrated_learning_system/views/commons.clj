(ns integrated-learning-system.views.commons
  (:use hiccup.core)
  (:require
    [integrated-learning-system.views.layouts :refer [navbar]]
    [hiccup.page :refer [include-css]]))


(defonce
  common-vendor-js-scripts
  ["https://cdn.jsdelivr.net/npm/@creativebulma/bulma-tagsinput@1.0.3/dist/js/bulma-tagsinput.min.js"
   "https://cdn.jsdelivr.net/npm/ramda@0.28.0/dist/ramda.min.js"
   "https://cdn.jsdelivr.net/npm/htm@3.1.1/dist/htm.js"])

(defonce
  vendor-js-scripts
  {"preact"                         "https://cdn.jsdelivr.net/npm/preact@10.11.3/dist/preact.min.js"
   "preact/hooks"                   "https://cdn.jsdelivr.net/npm/preact@10.11.3/hooks/dist/hooks.umd.js"
   "xstate"                         "https://cdn.jsdelivr.net/npm/xstate@4.34.0/dist/xstate.js"
   "dayjs"                          "https://cdn.jsdelivr.net/npm/dayjs@1.11.5/dayjs.min.js"
   "dayjs/plugin/customParseFormat" "https://cdn.jsdelivr.net/npm/dayjs@1.11.5/plugin/customParseFormat.js"
   "dayjs/plugin/isoWeek"           "https://cdn.jsdelivr.net/npm/dayjs@1.11.5/plugin/isoWeek.js"
   "dayjs/plugin/isSameOrAfter"     "https://cdn.jsdelivr.net/npm/dayjs@1.11.5/plugin/isSameOrAfter.js"
   "vanillajs-datepicker"           "https://cdn.jsdelivr.net/npm/vanillajs-datepicker@1.2.0/dist/js/datepicker-full.min.js"
   "bulma-toast"                    "https://cdn.jsdelivr.net/npm/bulma-toast@2.4.2/dist/bulma-toast.min.js"
   "slugify"                        "https://cdn.jsdelivr.net/npm/slugify@1.6.5/slugify.min.js"})

(defonce
  vendor-stylesheets
  {"bulma-checkbox"       "https://cdn.jsdelivr.net/npm/bulma-checkbox@1.2.1/css/main.min.css"
   "bulma-radio"          "https://cdn.jsdelivr.net/npm/bulma-radio@1.2.0/css/main.min.css"
   "bulma-divider"        "https://cdn.jsdelivr.net/npm/@creativebulma/bulma-divider@1.1.0/dist/bulma-divider.min.css"
   "bulma-helpers"        "https://cdn.jsdelivr.net/npm/bulma-helpers@0.3.8/css/bulma-helpers.min.css"
   "vanillajs-datepicker" "https://cdn.jsdelivr.net/npm/vanillajs-datepicker@1.2.0/dist/css/datepicker.min.css"})

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
   ; CAVEAT: `include-css` not able to understand the link to Google Fonts stylesheet document
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
