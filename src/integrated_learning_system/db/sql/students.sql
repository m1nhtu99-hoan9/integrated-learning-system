-- :name all-students :? :*
SELECT a.username, au.first_name, au.last_name, au.personal_email, au.date_of_birth, au.phone_number
FROM student
         INNER JOIN account a on a.id = student.account_id
         INNER JOIN account_user au on a.id = au.account_id;

-- :name student-by-username :? :1
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
WHERE username = :username;

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
     -- https://www.hugsql.org/hugsql-in-detail/parameter-types/sql-value-list-parameters
WHERE student.id IN (:v*:student_ids);

-- :name -student-timetable-by-student-id :? :*
SELECT t.number    AS timeslot_number
     , cp.school_date
     , c.class_name
     , course.code AS course_code
     , course.course_name
FROM student
         INNER JOIN student_class sc ON student.id = sc.student_id
         INNER JOIN class c ON sc.class_id = c.id
         INNER JOIN course ON c.course_id = course.id
         INNER JOIN class_period cp ON c.id = cp.class_id
         INNER JOIN timeslot t ON cp.timeslot_id = t.id
WHERE student.id = :student_id
  AND cp.school_date BETWEEN :from_date AND :to_date
ORDER BY t.number, cp.school_date;
