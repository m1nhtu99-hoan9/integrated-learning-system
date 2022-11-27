-- :name -all-teachers :? :*
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
     -- https://www.hugsql.org/hugsql-in-detail/parameter-types/sql-value-list-parameters
WHERE teacher.id IN (:v*:teacher_ids);

-- :name -teacher-timetable-by-teacher-id :? :*
SELECT t.number    AS timeslot_number
     , cp.school_date
     , c.class_name
     , course.code AS course_code
     , course.course_name
FROM teacher
         INNER JOIN teacher_class sc ON teacher.id = sc.teacher_id
         INNER JOIN class c ON sc.class_id = c.id
         INNER JOIN course ON c.course_id = course.id
         INNER JOIN class_period cp ON c.id = cp.class_id
         INNER JOIN timeslot t ON cp.timeslot_id = t.id
WHERE teacher.id = :teacher_id
  AND cp.school_date BETWEEN :from_date AND :to_date
ORDER BY t.number, cp.school_date;