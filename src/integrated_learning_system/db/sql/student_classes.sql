-- :name -insert-student-class-if-not-exists :<! :1
INSERT INTO student_class (student_id, class_id)
VALUES (:student_id, :class_id)
ON CONFLICT (student_id, class_id) DO NOTHING
RETURNING *;

-- :name -insert-student-classes-if-not-exists :<! :*
WITH tc AS (
    INSERT INTO student_class (student_id, class_id)
        -- CAVEAT: hugsql tuple list expression, code editor mistakes it for syntax error
        --          https://www.hugsql.org/hugsql-in-detail/parameter-types/sql-tuple-list-parameters
        VALUES :t*:student_ids_and_class_ids
        ON CONFLICT (student_id, class_id) DO NOTHING
        RETURNING student_id)
SELECT student.id AS student_id
     , username
FROM student
         INNER JOIN account a ON a.id = student.account_id
         INNER JOIN tc ON student.id = tc.student_id;

-- :name -delete-student-classes-by-class-name :<! :*
DELETE
FROM student_class
    USING student, account
WHERE class_id IN (SELECT id FROM class WHERE class.class_name = :class_name)
  AND student_id = student.id
  AND student.account_id = account.id
RETURNING student_id, account.username;