-- :name all-students :? :*
SELECT a.username, au.first_name, au.last_name, au.personal_email, au.date_of_birth, au.phone_number
FROM student
         INNER JOIN account a on a.id = student.account_id
         INNER JOIN account_user au on a.id = au.account_id;

-- :name -students-by-usernames :? :*
SELECT student.id AS student_id,
       a.username,
       au.first_name,
       au.last_name,
       au.personal_email,
       au.date_of_birth,
       au.phone_number
FROM student
         INNER JOIN account a on a.id = student.account_id
         INNER JOIN account_user au on a.id = au.account_id
    -- https://www.hugsql.org/hugsql-in-detail/parameter-types/sql-value-list-parameters
WHERE username IN (:v*:usernames);

-- :name -students-by-student-ids :? :*
SELECT student.id AS student_id,
       a.username,
       au.first_name,
       au.last_name,
       au.personal_email,
       au.date_of_birth,
       au.phone_number
FROM student
         INNER JOIN account a on a.id = student.account_id
         INNER JOIN account_user au on a.id = au.account_id
WHERE student.id IN (:v * :student_ids);
