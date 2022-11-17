-- :name class-by-id :? :1
SELECT class.id           AS class_id
     , class_name
     , course_name
     , course.code        AS course_code
     , course.description AS course_description
     , course.status      AS course_status
FROM class INNER JOIN course ON course.id = class.course_id
WHERE class.id = :id;

-- :name -class-by-class-name :? :1
SELECT class.id           AS class_id
     , class_name
     , course_name
     , course.code        AS course_code
     , course.description AS course_description
     , course.status      AS course_status
FROM class INNER JOIN course ON course.id = class.course_id
WHERE class.class_name = :class_name;

-- :name classes-by-course-code :? :*
SELECT class.id           AS class_id
     , course.id          AS course_id
     , class_name
     , course_name
     , course.code        AS course_code
     , course.description AS course_description
     , course.status      AS course_status
FROM class INNER JOIN course ON course.id = class.course_id
WHERE course.code = :course_code;

-- :name -count-class-periods :? :1
SELECT COUNT(*)
FROM class_period
INNER JOIN class ON class_period.class_id = class.id
WHERE class_id = :class_id;

-- :name -class-class-periods-within-range :? :*
SELECT school_date, timeslot.number AS timeslot_number
FROM class_period
         INNER JOIN timeslot ON class_period.timeslot_id = timeslot.id
         INNER JOIN class ON class_period.class_id = class.id
WHERE class.class_name = :class_name
  AND school_date BETWEEN :from_date::DATE AND :to_date::DATE
ORDER BY school_date, timeslot_number;

-- :name -class-class-periods :? :*
SELECT school_date, timeslot.number AS timeslot_number
FROM class_period
         INNER JOIN timeslot ON class_period.timeslot_id = timeslot.id
         INNER JOIN class ON class_period.class_id = class.id
WHERE class.class_name = :class_name
ORDER BY school_date, timeslot_number;
