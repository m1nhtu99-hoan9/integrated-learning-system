-- :name all-students :? :*
SELECT a.username, au.first_name, au.last_name, au.personal_email, au.date_of_birth, au.phone_number
FROM student
         INNER JOIN account a on a.id = student.account_id
         INNER JOIN account_user au on a.id = au.account_id;

-- :name student-by-username :? :1
SELECT a.username, au.first_name, au.last_name, au.personal_email, au.date_of_birth, au.phone_number
FROM student
         INNER JOIN account a on a.id = student.account_id
         INNER JOIN account_user au on a.id = au.account_id
WHERE username = :username;
