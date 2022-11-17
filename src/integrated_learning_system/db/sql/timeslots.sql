-- :name -last-timeslot :? :1
SELECT id, number, start_at, duration_mins
FROM timeslot
ORDER BY number DESC
LIMIT 1;

-- :name -all-timeslots :? :*
SELECT id, number, start_at, duration_mins
FROM timeslot
ORDER BY number;

-- :name -add-timeslot! :returning-execute :1
INSERT INTO timeslot (id, number, start_at, duration_mins)
VALUES (:id::UUID, :number, :start_at::TIME, :duration_mins)
RETURNING *;