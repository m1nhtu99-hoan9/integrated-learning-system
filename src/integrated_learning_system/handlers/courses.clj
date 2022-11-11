(ns integrated-learning-system.handlers.courses
  (:require
    [com.brunobonacci.mulog :as mulog]
    [integrated-learning-system.db :as db]
    [integrated-learning-system.db.courses :as courses-db]
    [integrated-learning-system.handlers.commons.api :as api]
    [integrated-learning-system.specs :refer [spec-validate]]
    [integrated-learning-system.specs.requests.courses :as s-courses]
    [integrated-learning-system.utils.throwable :refer [exn->map]]
    [next.jdbc :as jdbc]))


;;-- POST handler

(defn- -validate-create-course [body-params]
  (try
    (let [body-spec ::s-courses/course-add-request,
          request (select-keys body-params [:course-code :course-name :description]),
          validation-errors (spec-validate body-spec
                                           s-courses/validation-messages
                                           request)]
      {:request         (when (nil? validation-errors)
                          request),
       :non-ok-response (some->> validation-errors
                                 (api/resp-422 "Validation errors."))})

    (catch Exception exn
      (mulog/log ::failed-validate-create-course
                 :exn (exn->map exn
                                (fn [trace-stack]
                                  (->> trace-stack (take 8) (into [])))))
      {:non-ok-response (api/resp-500 "Failed to process this request for creating course."
                                      nil)})))

(defn- -process-add-course [db-tx request]
  (try
    (let [{added-course ::db/result, error ::db/error} (courses-db/add-course! db-tx request),
          {:keys [code course-name description status]} added-course]
      (if (some? error)
        ; rollback db-tx, then returns code 422
        (do
          (.rollback db-tx)
          (api/resp-422 "Data conflicts." error))
        ; else, returns code 201
        (api/resp-201 (str "/courses/" code) {:course-code code,
                                              :course-name course-name,
                                              :description description,
                                              :status      status})))
    (catch Exception exn
      (let [{:keys [course-code course-name]} request,
            course-summary (str "'" course-code "' (" course-name ")")]

        (mulog/log ::failed-process-add-course
                   :exn (exn->map exn)
                   :request request)

        (api/resp-500 (str "Failed to process this request to create course " course-summary ".")
                      nil)))))

(defn create-course [req]
  (let [db-conn (get-in req [:services :db-conn])
        body-params (:body-params req)
        {:keys [non-ok-response request]} (-validate-create-course body-params)]
    ; TODO: validates auth headers, grants access to admin users only
    (if (some? non-ok-response)
      non-ok-response
      ; else: open `db-conn`, then begin transaction `db-tx`
      (jdbc/with-transaction [db-tx db-conn]
        (-process-add-course db-tx request)))))
