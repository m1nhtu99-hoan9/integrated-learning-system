(ns integrated-learning-system.handlers.classes.members
  (:require
    [com.brunobonacci.mulog :as mulog]
    [integrated-learning-system.db.classes :as classes-db]
    [integrated-learning-system.db.student-classes :as student-classes-db]
    [integrated-learning-system.db.students :as students-db]
    [integrated-learning-system.db.teacher-classes :as teacher-classes-db]
    [integrated-learning-system.db.teachers :as teachers-db]
    [integrated-learning-system.handlers.commons :refer [user-display-names]]
    [integrated-learning-system.handlers.commons.api :as api]
    [integrated-learning-system.specs :refer [spec-explanation->validation-result]]
    [integrated-learning-system.specs.requests.classes :as s-classes]
    [integrated-learning-system.utils.throwable :refer [exn->map]]
    [next.jdbc :as jdbc]))


;;region GET handlers

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

;;endregion

;;region PUT handlers

(defn- -validate-class-members-to-replace-exist [db-conn {req-teacher  :class-teacher
                                                          req-students :class-students}]
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

;;endregion
