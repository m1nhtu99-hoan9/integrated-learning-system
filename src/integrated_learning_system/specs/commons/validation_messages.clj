(ns integrated-learning-system.specs.commons.validation-messages
  (:require [clojure.string :as string]
            [integrated-learning-system.utils.datetime :refer [date-patterns]])
  (:import [org.apache.commons.lang3 StringUtils]))

(defn prop-string-nonempty [prop-name]
  (str prop-name " must not be blank."))

(defn prop-string-length-between [prop-name min-length max-length]
  (str prop-name "'s length must be between " min-length " and " max-length " character(s)."))

(defn prop-string-maxlength [prop-name max-length]
  (str prop-name "'s length must not exceed " max-length "."))

(defn prop-enum-invalid-value [prop-name valid-values actual-value]
  (let [formatted-valid-values (map #(str "'" % "'") valid-values)]
    (str "Invalid " prop-name " value: '" actual-value
         "'. Allowed values: " (string/join ", " formatted-valid-values) ".")))

(defn prop-date-invalid-value [prop-name actual-value]
  (str "Invalid date value for " prop-name ": '" actual-value
       "'. Allowed formats: " (string/join ", " date-patterns)))

(defn prop-time-invalid-value [prop-name actual-value expected-format]
  (str "Invalid time value for " prop-name ": '" actual-value
       ". Expected format: " expected-format))

(defn prop-string-email-invalid [prop-name actual-value]
  (let [for-snippet (if (.equals "email" prop-name)
                      StringUtils/EMPTY
                      (.concat " for " prop-name))]
    (str "Invalid email value" for-snippet ": '" actual-value "'.")))

(defn prop-string-phonenum-invalid [prop-name actual-value]
  (str "Invalid phone number value for " prop-name ": '" actual-value "'."))
