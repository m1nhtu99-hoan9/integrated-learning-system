(ns integrated-learning-system.handlers.classes
  (:require
    [com.brunobonacci.mulog :as mulog]
    [integrated-learning-system.db :as db]
    [integrated-learning-system.db.classes :as classes-db]
    [integrated-learning-system.db.student-classes :as student-classes-db]
    [integrated-learning-system.db.teacher-classes :as teacher-classes-db]
    [integrated-learning-system.db.teachers :as teachers-db]
    [integrated-learning-system.db.students :as students-db]
    [integrated-learning-system.handlers.commons :refer [user-display-names]]
    [integrated-learning-system.handlers.commons.api :as api]
    [integrated-learning-system.specs :refer [spec-validate spec-explanation->validation-result]]
    [integrated-learning-system.specs.requests.classes :as s-classes]
    [integrated-learning-system.utils.throwable :refer [exn->map]]
    [integrated-learning-system.utils.datetime :as dt]
    [next.jdbc :as jdbc]
    [java-time.api :as jt]))


;region GET handlers

(defn get-class-periods [{{from-date-txt :from-date, to-date-txt :to-date} :body-params,
                          {:keys [class-name]}                             :path-params,
                          {:keys [db-conn]}                                :services,
                          :keys                                            [coercion-problems]
                          :as                                              req}]
  (try
    (cond
      (some? coercion-problems) (api/resp-401 "Invalid request"
                                              (spec-explanation->validation-result s-classes/validation-messages coercion-problems)),
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

(defn get-class-members [{:as                          req
                          :keys                        [coercion-problems]
                          {:keys [db-conn]}            :services
                          {{:keys [class-name]} :path} :parameters}]
  (try
    (cond
      (some? coercion-problems) (api/resp-401 "Invalid request"
                                              (spec-explanation->validation-result s-classes/validation-messages
                                                                                   coercion-problems)),
      (nil? db-conn) (api/resp-302 "/api/ping"),
      :else (let [db-query-params {:class-name class-name}
                  students (classes-db/class-students-by-class-name db-conn db-query-params)
                  teacher (classes-db/class-teacher-by-class-name db-conn db-query-params)]
              (api/resp-200 {:class-teacher  (merge (dissoc teacher :teacher-id)
                                                    (user-display-names teacher))
                             :class-students (for [student students]
                                               (merge (dissoc student :student-id)
                                                      (user-display-names student)))})))
    (catch Exception exn
      (mulog/log ::failed-get-class-members
                 :exn (exn->map exn (fn [stack] (->> stack (take 8) (into []))))
                 :req-arg (select-keys req [:body-params :path-params]))
      (api/resp-500 (str "Failed to process this request getting class member(s) of '" class-name "'.")
                    nil))))
;endregion

;region POST handlers

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
                      (classes-db/class-periods-of-class db-tx {:class-name class-name
                                                                :from-date  start-date
                                                                :to-date    end-date}))))
    (catch Exception exn
      (mulog/log ::failed-organise-class-periods
                 :exn (exn->map exn (fn [stack] (->> stack (take 8) (into []))))
                 :req-arg (select-keys req [:body-params :path-params]))
      (api/resp-500 (str "Failed to organise class period(s) for '" class-name "'.")
                    nil))))

;endregion

;endregion

;region PUT handlers

(defn- -validate-class-members-to-replace-exist [db-conn {req-teacher  :class-teacher
                                                          req-students :class-students
                                                          :as          body-params}]
  (let [teacher-username (:username req-teacher)
        teacher (teachers-db/teacher-by-username db-conn {:username teacher-username})
        req-student-usernames (mapv :username req-students),
        ; TODO: validate against non-string values among req-student-usernames
        students (students-db/students-by-usernames db-conn {:usernames req-student-usernames}),
        student-username-groups (group-by :username students),
        teacher-errors (when (nil? teacher)
                         {teacher-username (str "No teachers with username '" teacher-username "' existed in the system.")}),
        student-errors (transduce (comp (filter #(->> %
                                                      (contains? student-username-groups)
                                                      not))
                                        (map #([% (str "No students with username '" % "' existed in the system.")])))
                                  merge
                                  {}
                                  req-student-usernames),
        errors (merge teacher-errors student-errors)]
    {:non-ok-response (when-not (empty? errors)
                        (api/resp-422 "Data conflicts" errors)),
     :future-teacher  teacher,
     :future-students students}))


(defn replace-class-members [{:keys                        [coercion-problems body-params], :as req,
                              {:keys [db-conn]}            :services,
                              {{:keys [class-name]} :path} :parameters}]
  (try
    (let [{:as this-class, :keys [class-id]} (some-> db-conn (classes-db/class-by-class-name {:class-name class-name}))]
      (cond
        (some? coercion-problems) (api/resp-401 "Invalid request"
                                                (spec-explanation->validation-result s-classes/validation-messages
                                                                                     coercion-problems)),
        (nil? db-conn) (api/resp-302 "/api/ping"),
        (nil? this-class) (api/resp-401 {:class-name [(str "Class with name '" class-name "' did not exist.")]})
        :else
        (let [{:keys [non-ok-response future-teacher future-students]} (-validate-class-members-to-replace-exist
                                                                         db-conn
                                                                         body-params),
              {future-teacher-id :teacher-id} future-teacher,
              future-student-ids (mapv :student-id future-students)]
          (if (some? non-ok-response)
            non-ok-response
            ; else: process replacing class members
            (jdbc/with-transaction
              [db-tx db-conn]
              (let [->student-id-username-map (fn [student-classes]
                                                (transduce (map #(vector (:student-id %) (:username %)))
                                                           merge
                                                           {}
                                                           student-classes)),
                    removed-class-teachers (teacher-classes-db/delete-teacher-classes-by-class-name!
                                             db-tx
                                             {:class-name class-name}),
                    {removed-teacher-id       :teacher-id
                     removed-teacher-username :username} (first removed-class-teachers),
                    removed-class-students (student-classes-db/delete-student-classes-by-class-name!
                                             db-tx
                                             {:class-name class-name}),
                    removed-student-id-usernames (->student-id-username-map removed-class-students),
                    removed-student-ids (->> removed-student-id-usernames keys vec),
                    {added-teacher-id       :teacher-id
                     added-teacher-username :username} (teacher-classes-db/safe-insert-teacher-class!
                                                         db-tx {:class-id   class-id
                                                                :teacher-id future-teacher-id}),
                    added-class-students (student-classes-db/insert-students-to-class!
                                           db-tx {:class-id    class-id
                                                  :student-ids future-student-ids}),
                    added-student-id-usernames (->student-id-username-map added-class-students),
                    added-student-ids (->> added-student-id-usernames keys vec),
                    all-student-ids (into #{} (concat removed-student-ids added-student-ids))] ; transform to set to filter out duplicate
                (api/resp-200
                  {:changes
                   {:class-teacher  (when-not (= added-teacher-id removed-teacher-id)
                                      {:removed removed-teacher-username
                                       :added   added-teacher-username}),
                    :class-students (reduce
                                      (fn [changes student-id]
                                        (let [is-in? (fn [id ids]
                                                       (some? (some #{id} ids)))
                                              removed? (is-in? student-id removed-student-ids),
                                              added? (is-in? student-id added-student-ids)]
                                          (cond
                                            (and removed? added?) changes,
                                            removed? (update changes
                                                             :removed
                                                             #(conj % (removed-student-id-usernames :student-id))),
                                            added? (update changes
                                                           :added
                                                           #(conj % (added-student-id-usernames :student-id))))))
                                      {:removed [], :added []}
                                      all-student-ids)}})))))))

    (catch Exception exn
      (mulog/log ::failed-replace-class-members
                 :exn (exn->map exn (fn [stack] (->> stack (take 8) (into []))))
                 :req-arg (select-keys req [:body-params :path-params]))
      (api/resp-500 (str "Failed to process this request managing class members(s) of '" class-name "'.")
                    nil))))

;endregion

;region others

(defn move-class-period [_]
  (api/resp-501))

;endregion