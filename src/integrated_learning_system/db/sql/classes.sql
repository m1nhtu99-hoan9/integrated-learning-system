-- :name class-by-id :? :1
SELECT class.id           AS class_id
     , class_name
     , course_name
     , course.code        AS course_code
     , course.description AS course_description
     , course.status      AS course_status
FROM class INNER JOIN course on course.id = class.course_id
WHERE class.id = :id;

-- :name classes-by-course-code :? :*
SELECT class.id           AS class_id
     , course.id          AS course_id
     , class_name
     , course_name
     , course.code        AS course_code
     , course.description AS course_description
     , course.status      AS course_status
FROM class INNER JOIN course on course.id = class.course_id
WHERE course.code = :course_code;