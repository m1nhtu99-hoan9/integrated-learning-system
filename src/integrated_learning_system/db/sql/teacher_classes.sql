-- :name -insert-teacher-class-if-not-exists :<! :1
WITH tc AS (
    INSERT INTO teacher_class (teacher_id, class_id)
        VALUES (:teacher_id, :class_id)
        ON CONFLICT (teacher_id, class_id) DO NOTHING
        RETURNING teacher_id)
SELECT teacher.id AS teacher_id
     , username
FROM teacher
         INNER JOIN account a ON a.id = teacher.account_id
         INNER JOIN tc ON teacher.id = tc.teacher_id;

-- :name -delete-teacher-classes-by-class-name :<! :*
DELETE
FROM teacher_class tc
    USING teacher, account
WHERE class_id IN (SELECT id FROM class WHERE class.class_name = :class_name)
  AND teacher_id = teacher.id
  AND teacher.account_id = account.id
RETURNING teacher_id, account.username;
