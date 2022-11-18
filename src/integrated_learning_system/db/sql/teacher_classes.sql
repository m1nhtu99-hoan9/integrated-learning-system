-- :name -insert-teacher-class-if-not-exists
INSERT INTO teacher_class (teacher_id, class_id)
VALUES (:teacher_id, :class_id)
ON CONFLICT (teacher_id, class_id) DO NOTHING;