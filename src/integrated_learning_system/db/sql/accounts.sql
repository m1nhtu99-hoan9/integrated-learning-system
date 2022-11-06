-- :name account-by-username :? :1
-- :doc Find account by username
SELECT * FROM account a
INNER JOIN account_user au on a.id = au.account_id
WHERE username = :username;

-- :name -add-account-user! :!
INSERT INTO account_user (account_id, first_name, last_name, date_of_birth, personal_email, phone_number, is_admin)
VALUES (:account_id::UUID, :first_name, :last_name, date(:date_of_birth), :personal_email, :phone_number, :is_admin)