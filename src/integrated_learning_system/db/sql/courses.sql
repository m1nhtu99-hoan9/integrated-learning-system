-- :name course-by-code :? :1
SELECT * FROM course WHERE code = :code;

-- :name -courses-by-filters :? :*
SELECT * FROM course
WHERE code LIKE :code_pattern
  AND course_name LIKE :course_name_pattern;