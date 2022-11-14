(ns integrated-learning-system.utils.datetime
  (:require [clojure.algo.generic.functor :refer [fmap]]
            [com.brunobonacci.mulog :as mulog]
            [java-time.api :as jt])
  (:import [clojure.lang Keyword]
           [java.time DateTimeException LocalDate LocalTime]
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

(defonce time-patterns
         {:time/extended             "HH:mm:ss.SSS"
          :time/without-milliseconds "HH:mm:ss"
          :time/without-seconds      "HH:mm"})

(defonce
  ^:private local-time-formatters
  (fmap #(jt/formatter % {:resolver-style :strict}) time-patterns))

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
                   :string-arg string
                   :exn-message exn-message)
        {::error exn-message}))))


(defn string->local-time
  ([^CharSequence string, ^Keyword pattern-key]
   (try
     (let [fmt (get local-time-formatters pattern-key)]
       (jt/local-time fmt string))

     (catch Exception exn
       (let [exn-message (-> exn Throwable->map :cause)]
         (mulog/log ::failed-string->local-time
                    :string-arg string
                    :pattern-key-arg pattern-key
                    :exn-message exn-message)
         {::error exn-message}))))
  ([string]
   (string->local-time string :time/extended)))


(defn local-time->string
  ([^LocalTime local-time, ^Keyword pattern-key]
   (try
     (let [fmt (get local-time-formatters pattern-key)]
       (jt/format fmt local-time))

     (catch Exception exn
       (let [exn-message (-> exn Throwable->map :cause)]
         (mulog/log ::failed-local-time->string
                    :local-time-arg local-time
                    :pattern-key-arg pattern-key
                    :exn-message exn-message)
         {::error exn-message}))))
  ([local-time]
   (local-time->string local-time :time/extended)))


(defn aligned-week-of-year
  ; compliant with https://en.wikipedia.org/wiki/ISO_8601
  ([^LocalDate date]
   (.get date (jt/field :aligned-week-of-year)))
  ([] (aligned-week-of-year (jt/local-date))))


(defn with-week-date
  ([^LocalDate date, {:keys [week-of-year day-of-week]
                      :or   {day-of-week 1}}]
   (try
     (let [d (if (nil? week-of-year)
               date
               (.with date
                      (jt/field :aligned-week-of-year)
                      week-of-year))]
       (.with d
              (jt/field :day-of-week)
              day-of-week))
     (catch DateTimeException date-time-exn
       {::error (ex-message date-time-exn)})))
  ([props] (with-week-date (jt/local-date)
                           props)))
