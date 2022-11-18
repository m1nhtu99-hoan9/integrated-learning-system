(ns integrated-learning-system.handlers.classes
  (:require
    [com.brunobonacci.mulog :as mulog]
    [integrated-learning-system.db :as db]
    [integrated-learning-system.db.classes :as classes-db]
    [integrated-learning-system.handlers.commons.api :as api]
    [integrated-learning-system.specs :refer [spec-validate spec-explanation->validation-result]]
    [integrated-learning-system.specs.requests.classes :as s-classes]
    [integrated-learning-system.utils.throwable :refer [exn->map]]
    [integrated-learning-system.utils.datetime :as dt]
    [next.jdbc :as jdbc]
    [java-time.api :as jt]))


;;-- GET handler

(defn get-class-periods [{{from-date-txt :from-date, to-date-txt :to-date} :body-params,
                          {:keys [class-name]} :path-params,
                          {:keys [db-conn]} :services,
                          :keys [coercion-problems]
                          :as req}]
  (try
    (cond
      (some? coercion-problems) (api/resp-401 "Invalid request"
                                              (spec-explanation->validation-result s-classes/validation-messages coercion-problems)),
      (nil? db-conn) (api/resp-302 "/api/ping"),
      :else (let [from-date (some-> from-date-txt dt/string->local-date),
                  to-date (some-> to-date-txt dt/string->local-date),
                  class-periods (classes-db/class-periods-of-class db-conn
                                                                   {:class-name class-name
                                                                    :from-date from-date
                                                                    :to-date to-date})]
              (api/resp-200
                (for [class-period class-periods]
                  (update class-period :school-date #(jt/format "dd/MM/uuuu" %))))))
    (catch Exception exn
      (mulog/log ::failed-get-class-periods
                 :exn (exn->map exn (fn [stack] (->> stack (take 8) (into []))))
                 :req-arg (select-keys req [:body-params :path-params]))
      (api/resp-500 (str "Failed to process this request getting class period(s) of '" class-name "'.")
                    nil))))

(defn get-class-members [{:as req
                          :keys [coercion-problems]
                          {:keys [db-conn]} :services
                          {{:keys [class-name]} :path} :parameters}]
  (try
    (cond
      (some? coercion-problems) (api/resp-401 "Invalid request"
                                  (spec-explanation->validation-result s-classes/validation-messages coercion-problems)),
      (nil? db-conn) (api/resp-302 "/api/ping"),
      :else (let [db-query-params {:class-name class-name}
                  students (classes-db/class-students-by-class-name db-conn db-query-params)
                  teacher (classes-db/class-teacher-by-class-name db-conn db-query-params)]
              (api/resp-200 {:class-teacher teacher
                             :class-students students})))
    (catch Exception exn
      (mulog/log ::failed-get-class-members
                 :exn (exn->map exn (fn [stack] (->> stack (take 8) (into []))))
                 :req-arg (select-keys req [:body-params :path-params]))
      (api/resp-500 (str "Failed to process this request getting class member(s) of '" class-name "'.")
                    nil))))

;;-- POST handler

;region create-class

(defn- -validate-create-class [body-params]
  (try
    (let [body-spec ::s-classes/class-add-request,
          request (select-keys body-params [:course-code :class-name]),
          validation-errors (spec-validate body-spec
                                           s-classes/validation-messages
                                           request)]
      {:request         (when (nil? validation-errors)
                          request),
       :non-ok-response (some->> validation-errors
                                 (api/resp-422 "Validation errors."))})

    (catch Exception exn
      (mulog/log ::failed-validate-create-class
                 :exn (exn->map exn
                                (fn [trace-stack]
                                  (->> trace-stack (take 8) (into [])))))
      {:non-ok-response (api/resp-500 "Failed to process this request for creating new class."
                                      nil)})))

(defn- -process-add-class [db-tx request]
  (try
    (let [{added-class ::db/result, error ::db/error} (classes-db/add-class! db-tx request),
          {:keys [class-name]} added-class]
      (if (some? error)
        ; rollback db-tx, then returns code 422
        (do
          (.rollback db-tx)
          (api/resp-422 "Data conflicts." error))
        ; else, returns code 201
        (api/resp-201 (str "/classes/" class-name)
                      (dissoc added-class :class-id))))

    (catch Exception exn
      (let [{:keys [class-name course-code]} request,
            class-summary (str "'" class-name "' for course '" course-code "'")]

        (mulog/log ::failed-process-add-class
                   :exn (exn->map exn)
                   :request request)

        (api/resp-500 (str "Failed to process this request to create new class " class-summary ".")
                      nil)))))

(defn create-class [req]
  (let [db-conn (get-in req [:services :db-conn])
        body-params (:body-params req)
        {:keys [non-ok-response request]} (-validate-create-class body-params)]
    ; TODO: validates auth headers, grants access to admin users only
    (if (some? non-ok-response)
      non-ok-response
      ; else: open `db-conn`, then begin transaction `db-tx`
      (jdbc/with-transaction
        [db-tx db-conn]
        (-process-add-class db-tx request)))))

;endregion

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
        (classes-db/add-class-periods-for-class!
          db-tx
          {:class-name class-name, :class-periods class-period-combinations})
        (api/resp-201 (str "/api/v1/classes/" class-name "/periods/")
          (classes-db/class-periods-of-class db-tx {:class-name      class-name
                                                    :from-date start-date
                                                    :to-date   end-date}))))
    (catch Exception exn
     (mulog/log ::failed-organise-class-periods
                :exn (exn->map exn (fn [stack] (->> stack (take 8) (into []))))
                :req-arg (select-keys req [:body-params :path-params]))
     (api/resp-500 (str "Failed to organise class period(s) for '" class-name "'.")
                   nil))))

;endregion

(defn assign-teacher-to-class [req]
  (api/resp-501))

(defn add-student-to-class [req]
  (api/resp-501))

(defn move-class-period [_]
  (api/resp-501))