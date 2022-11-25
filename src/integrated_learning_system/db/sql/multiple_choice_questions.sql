-- :name -add-multiple-choice-question! :>! :1
WITH q AS (INSERT INTO question (title)
    VALUES (:question_title)
    RETURNING id)
INSERT
INTO multiple_choice_question (question_id, body)
VALUES ((SELECT id FROM q FETCH FIRST 1 ROW ONLY), :question_json_body)
RETURNING question_id;


-- :name -multiple-choice-questions-by-due-class-period-id :? :*
SELECT sq.question_id
     , q.title  AS question_title
     , mcq.body AS question_body
     , sq.schoolwork_id
FROM multiple_choice_question mcq
         INNER JOIN question q ON mcq.question_id = q.id
         INNER JOIN schoolwork_question sq ON mcq.question_id = sq.question_id
         INNER JOIN schoolwork s ON sq.schoolwork_id = s.id
         LEFT JOIN classwork c ON s.id = c.schoolwork_id
         LEFT JOIN homework h ON s.id = h.schoolwork_id
WHERE (c.due_class_period_id = :due_class_period_id)
   OR (h.due_class_period_id = :due_class_period_id);
