(ns integrated-learning-system.handlers.classes
  (:require
    [com.brunobonacci.mulog :as mulog]
    [integrated-learning-system.db :as db]
    [integrated-learning-system.db.classes :as classes-db]
    [integrated-learning-system.handlers.commons.api :as api]
    [integrated-learning-system.specs :refer [spec-validate]]
    [integrated-learning-system.specs.requests.classes :as s-classes]
    [integrated-learning-system.utils.throwable :refer [exn->map]]
    [next.jdbc :as jdbc]))


;;region POST handlers

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

;;endregion
