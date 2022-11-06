(ns integrated-learning-system.handlers.accounts
  (:require
    [clojure.spec.alpha :as s]
    [com.brunobonacci.mulog :as mulog]
    [next.jdbc :as jdbc]
    [integrated-learning-system.db :as db]
    [integrated-learning-system.db.accounts :as accounts-db]
    [integrated-learning-system.db.teachers :as teachers-db]
    [integrated-learning-system.db.students :as students-db]
    [integrated-learning-system.specs :refer [spec-validate]]
    [integrated-learning-system.specs.requests.accounts :as s-accounts]
    [integrated-learning-system.handlers.commons.api :as api])
  (:import [org.apache.commons.lang3 NotImplementedException]))


;;-- GET handler

(defn get-account-by-username [req]
  (api/resp-501))

;;-- POST handler

(defn- -coerce-to-account-add-request [body-params]
  (let [body-spec ::s-accounts/account-add-request,
        account-add-request (s-accounts/body-params->account-add-request body-params),
        errors (spec-validate body-spec
                              s-accounts/validation-messages
                              account-add-request
                              :orig-candidate body-params)]
    {:invalid-body-errors errors,
     :request             account-add-request}))

(defn- -validate-account-add-request [{:as request, :keys [role]}]
  (let [payload-spec (s-accounts/select-account-add-payload-spec role),
        validation-errors (spec-validate payload-spec
                                         s-accounts/validation-messages
                                         request),
        ; CAVEAT: https://stackoverflow.com/a/49056441
        ; `conformed-payload` is not 100% guaranteed to be coerced using defined conformers.
        conformed-payload (if (some? validation-errors)
                            nil
                            (s/conform payload-spec request))]
    {:validation-errors validation-errors
     :conformed-payload conformed-payload}))


(defn- -process-add-teacher [db-tx account-id {:as conformed-body-params, :keys [username]}]
  (try
    (accounts-db/add-account-user! db-tx (assoc conformed-body-params :account-id account-id))
    (teachers-db/add-teacher! db-tx {:account-id account-id})
    (api/resp-201 (str "/accounts/" username))

    (catch Exception exn
      (.rollback db-tx)
      (mulog/log ::failed-process-add-teacher
                 :exn exn
                 :account-id account-id
                 :request-body conformed-body-params)
      (api/resp-500 (str "Failed to register a teacher account with username [" username "].")
                    nil))))

(defn- -process-add-student [db-tx account-id {:as conformed-body-params, :keys [username]}]
  (try
    (accounts-db/add-account-user! db-tx (assoc conformed-body-params :account-id account-id))
    (students-db/add-student! db-tx {:account-id account-id})
    (api/resp-201 (str "/accounts/" username))

    (catch Exception exn
      (.rollback db-tx)
      (mulog/log ::failed-process-add-student
                 :exn exn
                 :account-id account-id
                 :request-body conformed-body-params)
      (api/resp-500 (str "Failed to register a student account with username [" username "].")
                    nil))))

(defn -process-add-admin [db-tx account-id {:as conformed-body-params, :keys [username]}]
  (try
    (accounts-db/add-account-user! db-tx (assoc conformed-body-params :account-id account-id
                                                                      :is-admin true))
    (api/resp-201 (str "/accounts/" username))

    (catch Exception exn
      (.rollback db-tx)
      (mulog/log ::failed-process-add-admin
                 :exn exn
                 :account-id account-id
                 :request-body conformed-body-params)
      (api/resp-500 (str "Failed to register an admin account with username [" username "].")
                    nil))))

(defn- -validate-register-account [body-params]
  (try
    (let [{:keys [invalid-body-errors request]} (-coerce-to-account-add-request body-params)]
      (if (some? invalid-body-errors)
        {:non-ok-response (api/resp-401 "Invalid account registration request body."
                                        invalid-body-errors)}
        ; else: check for schema validation errors
        (let [{:keys [role]} request,
              {:keys [validation-errors, conformed-payload]} (-validate-account-add-request request)]
          {:non-ok-response (some->> validation-errors
                                     (api/resp-422 "Validation errors."))
           :role            role
           :payload         conformed-payload})))

    (catch Exception exn
      (mulog/log ::failed-validate-register-account
                 :exn exn
                 :body-params body-params)
      {:non-ok-response (api/resp-500 (str "Failed to process this account registration request.")
                                      nil)})))

(defn register-account [req]
  (let [db-conn (get-in req [:services :db-conn]),
        body-params (:body-params req),
        {:keys [non-ok-response, role, payload]} (-validate-register-account body-params)]
    (if (some? non-ok-response)
      non-ok-response
      ; else: open `db-conn`, then begin transaction `db-tx`
      (jdbc/with-transaction [db-tx db-conn]
                             (let [{added-account ::db/result, account-error ::db/error} (accounts-db/add-account! db-tx payload),
                                   {account-id :id} added-account]
                               (if (some? account-error)
                                 ; rollback, then returns code 422
                                 (do
                                   (.rollback db-tx)
                                   (api/resp-422 "Data duplication" account-error))
                                 ; else, process with adding
                                 (case role
                                   ; :unknown has been filtered out by validation
                                   :admin (-process-add-admin db-tx account-id payload)
                                   :teacher (-process-add-teacher db-tx account-id payload)
                                   :student (-process-add-student db-tx account-id payload))))))))
