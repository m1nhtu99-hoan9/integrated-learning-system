(ns integrated-learning-system.views.templates.classes.commons)


(defonce
   common-vendor-js-scripts
  ["https://cdn.jsdelivr.net/npm/@creativebulma/bulma-tagsinput@1.0.3/dist/js/bulma-tagsinput.min.js"
   "https://cdn.jsdelivr.net/npm/ramda@0.28.0/dist/ramda.min.js"
   "https://cdn.jsdelivr.net/npm/htm@3.1.1/dist/htm.js"
   "https://cdn.jsdelivr.net/npm/preact@10.11.3/dist/preact.min.js"
   "https://cdn.jsdelivr.net/npm/preact@10.11.3/hooks/dist/hooks.umd.js"])

(defonce
  vendor-js-scripts
  {"vanillajs-datepicker" "https://cdn.jsdelivr.net/npm/vanillajs-datepicker@1.2.0/dist/js/datepicker-full.min.js"
   "slugify"              "https://cdn.jsdelivr.net/npm/slugify@1.6.5/slugify.min.js"})

(defonce
  vendor-stylesheets
  {"bulma-checkbox"       "https://cdn.jsdelivr.net/npm/bulma-checkbox@1.2.1/css/main.min.css"
   "bulma-radio"          "https://cdn.jsdelivr.net/npm/bulma-radio@1.2.0/css/main.min.css"
   "bulma-helpers"        "https://cdn.jsdelivr.net/npm/bulma-helpers@0.3.8/css/bulma-helpers.min.css"
   "vanillajs-datepicker" "https://cdn.jsdelivr.net/npm/vanillajs-datepicker@1.2.0/dist/css/datepicker.min.css"})
