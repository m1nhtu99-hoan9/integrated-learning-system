(ns integrated-learning-system.handlers.timeslots
  (:require
    [com.brunobonacci.mulog :as mulog]
    [next.jdbc :as jdbc]
    [integrated-learning-system.utils.throwable :refer [exn->map]]
    [integrated-learning-system.db :as db]
    [integrated-learning-system.db.timeslots :as timeslots-db]
    [integrated-learning-system.specs :refer [spec-validate]]
    [integrated-learning-system.specs.requests.timeslots :as s-timeslots :refer [body-params->timeslot-add-request]]
    [integrated-learning-system.handlers.commons.api :as api]))


;;-- POST handler

;region timeslot-add-request

(defn- -coerce-to-timeslot-add-request [body-params]
  (let [request (body-params->timeslot-add-request body-params),
        errors (spec-validate ::s-timeslots/timeslot-add-request
                              s-timeslots/validation-messages
                              request
                              :orig-candidate body-params)]
    {:invalid-body-errors errors,
     :request             request}))

(defn- -validate-insert-timeslot [body-params]
  (try
    (let [{:keys [invalid-body-errors request]} (-coerce-to-timeslot-add-request body-params)]
      {:non-ok-response (some->> invalid-body-errors
                                 (api/resp-401 "Invalid time-slot insertion request body.")),
       :request (when (nil? invalid-body-errors)
                  request)})

    (catch Exception exn
      (mulog/log ::failed-validate-insert-timeslot
                 :exn exn
                 :body-params body-params)
      {:non-ok-response (api/resp-500 (str "Failed to process this time-slot insertion request.")
                                      nil)})))

(defn insert-timeslot [req]
  (let [db-conn (get-in req [:services :db-conn])
        body-params (:body-params req)
        {:keys [non-ok-response request]} (-validate-insert-timeslot body-params)]
    ; TODO: validates auth headers, grants access to admin users only
    (if (some? non-ok-response)
      non-ok-response
      ; else: open `db-conn`, then begin transaction `db-tx`
      (jdbc/with-transaction [db-tx db-conn]
        (try
          (let [{added-timeslot ::db/result, error ::db/error} (timeslots-db/add-timeslot! db-tx request),
                {:keys [number]} added-timeslot]
            (if (some? error)
              ; rollback db-tx, then returns code 422
              (do
                (.rollback db-tx)
                (api/resp-422 "Data conflicts" error))
              ; else, returns code 201
              (api/resp-201 (str "/timeslots/" number)
                            (select-keys added-timeslot [:number :start-at :duration-mins]))))

          (catch Exception exn
            (mulog/log ::failed-insert-timeslot
                       :exn (exn->map exn
                                      (fn [trace-stack]
                                        (-> trace-stack (take 8) (into []))))
                       :request request)
            (api/resp-500 "Failed to process this timeslot insertion request."
                          nil)))))))

;endregion