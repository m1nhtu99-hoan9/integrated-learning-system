(ns integrated-learning-system.handlers.classes.periods
  (:require
    [com.brunobonacci.mulog :as mulog]
    [next.jdbc :as jdbc]
    [integrated-learning-system.db.classes :as classes-db]
    [integrated-learning-system.handlers.commons.api :as api]
    [integrated-learning-system.specs :refer [spec-explanation->validation-result]]
    [integrated-learning-system.specs.requests.classes :as s-classes]
    [integrated-learning-system.utils.datetime :as dt]
    [integrated-learning-system.utils.throwable :refer [exn->map]]
    [java-time.api :as jt]))


;;region GET handlers

(defn get-class-periods [{{from-date-txt :from-date, to-date-txt :to-date} :body-params,
                          {:keys [class-name]}                             :path-params,
                          {:keys [db-conn]}                                :services,
                          :keys                                            [coercion-problems]
                          :as                                              req}]
  (try
    (cond
      (some? coercion-problems) (api/resp-401 "Invalid request"
                                              (spec-explanation->validation-result s-classes/validation-messages
                                                                                   coercion-problems)),
      (nil? db-conn) (api/resp-302 "/api/ping"),
      :else (let [from-date (some-> from-date-txt dt/string->local-date),
                  to-date (some-> to-date-txt dt/string->local-date),
                  class-periods (classes-db/class-periods-of-class db-conn
                                                                   {:class-name class-name
                                                                    :from-date  from-date
                                                                    :to-date    to-date})]
              (api/resp-200
                (for [class-period class-periods]
                  (update class-period :school-date #(jt/format "dd/MM/uuuu" %))))))
    (catch Exception exn
      (mulog/log ::failed-get-class-periods
                 :exn (exn->map exn (fn [stack] (->> stack (take 8) (into []))))
                 :req-arg (select-keys req [:body-params :path-params]))
      (api/resp-500 (str "Failed to process this request getting class period(s) of '" class-name "'.")
                    nil))))

;;endregion

;;region POST handlers

;region organise-class-periods

(defn- -class-period-combinations [date-range weekly-selections]
  (for [date date-range,
        weekly-selection weekly-selections,
        :let [{:keys [day-of-week-num timeslot-number]} weekly-selection]
        :when (= day-of-week-num
                 (jt/as date :day-of-week))]
    {:school-date  date
     :timeslot-num timeslot-number}))

(defn organise-class-periods [{:as                                  req,
                               {start-date-txt    :start-date
                                end-date-txt      :end-date
                                weekly-selections :weekly-schedule} :body-params,
                               {:keys [class-name]}                 :path-params}]
  (try
    (let [db-conn (get-in req [:services :db-conn]),
          start-date (dt/string->local-date start-date-txt),
          end-date (dt/string->local-date end-date-txt),
          date-range (apply vector (dt/date-range start-date end-date)),
          class-period-combinations (-> (-class-period-combinations date-range weekly-selections))]
      (jdbc/with-transaction
        [db-tx db-conn]
        (let [_ (classes-db/add-class-periods-for-class!
                  db-tx
                  {:class-name class-name, :class-periods class-period-combinations}),
              results (classes-db/class-periods-of-class db-tx {:class-name class-name
                                                                :from-date  start-date
                                                                :to-date    end-date})]
          (api/resp-201 (str "/api/v1/classes/" class-name "/periods/")
                        (into [] results)))))

    (catch Exception exn
      (mulog/log ::failed-organise-class-periods
                 :exn (exn->map exn (fn [stack] (->> stack (take 15) (into []))))
                 :req-arg (select-keys req [:body-params :path-params]))
      (api/resp-500 (str "Failed to organise class period(s) for '" class-name "'.")
                    nil))))
;endregion

;;endregion
