-- :name -insert-student-class-if-not-exists
INSERT INTO student_class (student_id, class_id)
VALUES (:student_id, :class_id)
ON CONFLICT (student_id, class_id) DO NOTHING;