-- :name -add-homework! :<! :1
WITH swk AS (
    INSERT INTO schoolwork (class_period_id, course_id, course_session_id, total_score)
        VALUES (:class_period_id, :course_id, :course_session_id, :total_score)
        RETURNING id)
INSERT
INTO homework (schoolwork_id, due_class_period_id)
VALUES ((SELECT id FROM swk FETCH FIRST 1 ROW ONLY), :due_class_period_id)
RETURNING schoolwork_id;
