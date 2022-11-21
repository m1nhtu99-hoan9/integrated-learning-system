-- :name all-teachers :? :*
SELECT a.username, au.first_name, au.last_name, au.personal_email, au.date_of_birth, au.phone_number
FROM teacher
         INNER JOIN account a on a.id = teacher.account_id
         INNER JOIN account_user au on a.id = au.account_id;

-- :name -teacher-by-username :? :1
SELECT teacher.id AS teacher_id,
       a.username,
       au.first_name,
       au.last_name,
       au.personal_email,
       au.date_of_birth,
       au.phone_number
FROM teacher
         INNER JOIN account a on a.id = teacher.account_id
         INNER JOIN account_user au on a.id = au.account_id
WHERE username = :username;

-- :name -teachers-by-teacher-ids :? :*
SELECT teacher.id AS teacher_id,
       a.username,
       au.first_name,
       au.last_name,
       au.personal_email,
       au.date_of_birth,
       au.phone_number
FROM teacher
         INNER JOIN account a on a.id = teacher.account_id
         INNER JOIN account_user au on a.id = au.account_id
WHERE teacher.id IN (:v * :teacher_ids);