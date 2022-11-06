(ns integrated-learning-system.utils.datetime
  (:require [com.brunobonacci.mulog :as mulog]
            [java-time.api :as jt])
  (:import [java.time LocalDate]
           [java.time.format DateTimeFormatterBuilder]))


(defonce
  ^{:doc "User-friendly date-only patterns"}
  date-patterns
  (vector "dd/MM/yyyy" "yyyy-MM-dd"))

(defonce
  ^{:doc     "Java8-specific string patterns for parsing string to LocalDate"
    :private true}
  local-date-patterns
  ; GOTCHA: 'u' denotes year, whereas 'y' denotes year-of-era
  ;   https://docs.oracle.com/javase/8/docs/api/java/time/format/DateTimeFormatter.html
  ;   https://docs.oracle.com/javase/8/docs/api/java/time/temporal/ChronoField.html#YEAR
  (vector "dd/MM/uuuu" "uuuu-MM-dd"))

(defonce
  ^:private local-date-formatter
  (let [builder (reduce (fn [builder pattern]
                          (.appendOptional builder (jt/formatter pattern)))
                        (DateTimeFormatterBuilder.)
                        local-date-patterns),
        fmt (.toFormatter builder)]
    (jt/formatter fmt {:resolver-style :strict})))

(defn string->local-date [^CharSequence string]
  (try
    (->> string
         (jt/local-date local-date-formatter))

    (catch Throwable exn
      (let [exn-message (-> exn Throwable->map :cause)]
        (mulog/log ::failed-string->local-date
                   :string string
                   :exn-message exn-message)
        {::error exn-message}))))
