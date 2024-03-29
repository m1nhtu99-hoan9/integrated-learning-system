(ns integrated-learning-system.db.accounts
  (:require
    [integrated-learning-system.utils.throwable :refer [exn->map]]
    [integrated-learning-system.db :as db]
    [integrated-learning-system.db.sql.commons :refer [path-to-sql]]
    [com.brunobonacci.mulog :as mulog]
    [next.jdbc.sql :as sql]
    [hugsql.core :as hugsql])
  (:import [java.util UUID]))


(defn account-by-username [db-conn {:keys [username]}]
  (comment "this fn gonna be redefined by hugsql."))
(defn -add-account-user! [db-conn account-user]
  (comment "this fn gonna be redefined by hugsql."))
(hugsql/def-db-fns (path-to-sql "accounts"))

(defn add-account! [db-conn
                    {:as account, :keys [username password]}]
  ; `db-conn` can be either transactable or connectible
  (try
    (if (some? (account-by-username db-conn {:username username}))
      {::db/error {:username (str "Account with username [" username "] already exists.")}}
      {::db/result (sql/insert! (db/with-snake-kebab-opts db-conn)
                                :account
                                {:id       (UUID/randomUUID)
                                 :username username
                                 :password password})})

    (catch Exception exn
      (mulog/log ::failed-add-account! :exn (exn->map exn) :account-arg account)
      ; rethrows to let next.jdbc handle rollback of transaction, if any
      (throw exn))))


(defn add-account-user! [db-conn
                         {:as   account-user
                          :keys [account-id is-admin phone-number personal-email date-of-birth first-name last-name]
                          :or   {account-id (UUID/randomUUID)
                                 is-admin   false}}]
  (try
    ;; CAVEAT: `(select-keys map [:personal-email])` would ignore `:personal-email` if it's not a `map` key.
    (let [record {:account_id     account-id
                  :phone_number   phone-number    ;; even if `phone-number` is `nil`, hugsql still requires the key to be present
                  :personal_email personal-email
                  :date_of_birth  date-of-birth
                  :first_name     first-name
                  :last_name      last-name
                  :is_admin       is-admin},
          added-account-user (-add-account-user! (db/with-snake-kebab-opts db-conn)
                                                 record)]
      {::db/result added-account-user})

    (catch Exception exn
      (mulog/log ::failed-add-account-user!
                 :exn (exn->map exn
                                #(->> % (take 9) (into [])))
                 :account-user-arg account-user)
      ; rethrows to let next.jdbc handle rollback of transaction, if any
      (throw exn))))
