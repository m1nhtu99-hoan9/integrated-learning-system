(ns integrated-learning-system.utils.datetime
  (:require [org.apache.commons.lang3.time.DateUtils :refer [*parse-date-strictly]]
            [com.brunobonacci.mulog :as mulog]
            [java-time.api :as jt])
  (:import [java.text ParseException]
           [java.util Date]
           [java.time LocalDate]
           [java.time.format DateTimeFormatterBuilder]))


(defonce ^:deprecated date-time-patterns
         (into-array (vector
                       "dd/MM/yyyy'T'HH:mm:ss" "dd/MM/yyyy HH:mm:ss"
                       "dd/MM/yyyy" "yyyy-MM-dd" "yyyy-MM-dd HH:mm:ss")))

(defonce
  ^{:doc "User-friendly date-only patterns"}
  date-patterns
  (vector "dd/MM/yyyy" "yyyy-MM-dd"))

(defonce
  ^{:doc     "Java8-specific string patterns for parsing string to LocalDate"
    :private true}
  local-date-patterns
  ; GOTCHA: 'u' denotes year, whereas 'y' denotes year-of-era
  ; https://docs.oracle.com/javase/8/docs/api/java/time/format/DateTimeFormatter.html
  ; https://docs.oracle.com/javase/8/docs/api/java/time/temporal/ChronoField.html#YEAR
  (vector "dd/MM/uuuu" "uuuu-MM-dd"))

(defonce
  ^:private local-date-formatter
  (let [builder (reduce (fn [builder pattern]
                          (.appendOptional builder (jt/formatter pattern)))
                        (DateTimeFormatterBuilder.)
                        local-date-patterns),
        fmt (.toFormatter builder)]
    (jt/formatter fmt {:resolver-style :strict})))

(defn ^:deprecated string->date [^String string]
  (try
    (*parse-date-strictly string date-time-patterns)

    (catch ParseException parse-exn
      (mulog/log ::failed-string->date
                 :exn parse-exn
                 :string string)
      nil)))

(defn string->local-date [^CharSequence string]
  (try
    (->> string
         (jt/local-date local-date-formatter))

    (catch Throwable exn
      (let [exn-map (Throwable->map exn)]
        (mulog/log ::failed-string->local-date
                   :string string
                   :exn exn-map)
        {::error (:cause exn-map)}))))

(defn ->date [obj]
  (cond
    (instance? Date obj) obj
    (instance? String obj) (string->date obj)
    :else nil))


