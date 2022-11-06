(ns integrated-learning-system.db.accounts
  (:require
    [integrated-learning-system.db :as db]
    [integrated-learning-system.db.sql.commons :refer [path-to-sql]]
    [org.apache.commons.lang3.StringUtils :refer [*equals]]
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
      (mulog/log ::failed-add-account! :exn exn :account-arg account)
      ; rethrows to let next.jdbc handle rollback of transaction, if any
      (throw exn))))


(defn add-account-user! [db-conn
                         {:as   account-user
                          :keys [account-id is-admin]
                          :or   {account-id (UUID/randomUUID)
                                 is-admin   false}}]
  (try
    (let [record (as-> account-user $
                       (select-keys $ [:phone-number :personal-email :date-of-birth :first-name :last-name])
                       (assoc $ :account-id account-id
                                :is-admin is-admin)
                       (db/transform-column-keys $)),
          added-account-user (-add-account-user! (db/with-snake-kebab-opts db-conn)
                                                 record)]
      {::db/result added-account-user})

    (catch Exception exn
      (mulog/log ::failed-add-account-user! :exn exn :account-user-arg account-user)
      ; rethrows to let next.jdbc handle rollback of transaction, if any
      (throw exn))))


(defn update-username! [db-conn current-username new-username]
  (sql/update! db-conn :account {:username new-username} {:username current-username}))

(defn update-password! [db-conn username current-password new-password]
  (if-let [account (account-by-username db-conn {:username username})]
    (if (*equals current-password (:password account))
      (sql/update! db-conn :account {:password new-password} {:username username})
      (str "Provided password doesn't match [" username "]'s current password."))
    (str "Account [" username "] not found.")))

(defn delete-account! [db-conn
                       {:as account, :keys [username]}]
  (sql/delete! db-conn :account {:username username}))
