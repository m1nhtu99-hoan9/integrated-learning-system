(ns integrated-learning-system.db.timeslots
  (:require
    [com.brunobonacci.mulog :as mulog]
    [hugsql.core :as hugsql]
    [integrated-learning-system.db :as db]
    [integrated-learning-system.db.sql.commons :refer [path-to-sql]]
    [integrated-learning-system.utils.datetime :as dt]
    [java-time.api :as jt])
  (:import [java.time LocalTime]
           [java.util UUID]))

(defn -last-timeslot [db-conn]
  (comment "this fn gonna be redefined by hugsql."))
(defn -all-timeslots [db-conn]
  (comment "this fn gonna be redefined by hugsql."))
(defn -add-timeslot! [db-conn {:keys [timeslot-id, number, start-at, duration-mins]}]
  (comment "this fn gonna be redefined by hugsql."))
(hugsql/def-db-fns (path-to-sql "timeslots"))


(defn- -stringify-start-at-time [^LocalTime start-at]
  (dt/local-time->string start-at :time/without-seconds))

(defn last-timeslot [db-conn]
  (some-> (-last-timeslot db-conn)
          (update :start-at #(jt/local-time %))))

(defn all-timeslots [db-conn]
  (some->> (-all-timeslots db-conn)
           (map (fn [timeslot]
                  (update timeslot :start-at #(.toLocalTime %))))))

(defn -add-first-timeslot! [db-conn {:as timeslot :keys [start-at duration-mins]}]
  (try
    (let [record (-> {:id (UUID/randomUUID), :number 1, :start-at start-at, :duration-mins duration-mins}
                     db/transform-column-keys),
          added-timeslot (-add-timeslot! (db/with-snake-kebab-opts db-conn)
                                         record)]
      {::db/result added-timeslot})
    (catch Exception exn
      (mulog/log ::failed-add-first-timeslot!
                 :exn (-> exn Throwable->map (dissoc :trace))
                 :timeslot-arg timeslot)
      ; rethrows to let next.jdbc handle rollback of transaction, if any
      (throw exn))))

(defn -append-timeslot! [db-conn
                         {:as latest-timeslot, latest-timeslot-number :number}
                         {:as timeslot, :keys [start-at duration-mins]}]
  (try
    (let [prev-start (:start-at latest-timeslot),
          prev-end (jt/plus prev-start
                            (jt/minutes (:duration-mins latest-timeslot)))]
      (if (jt/before? start-at prev-end)
        {::db/error (:start-at (str "Invalid start time: '" start-at
                                    "'. New time-slot is expected to not start before '"
                                    (-stringify-start-at-time start-at) "'."))},
        ; else: process adding
        (let [record (db/transform-column-keys {:id            (UUID/randomUUID)
                                                :number        (inc latest-timeslot-number)
                                                :start-at      start-at
                                                :duration-mins duration-mins}),
              added-timeslot (-add-timeslot! (db/with-snake-kebab-opts db-conn)
                                             record)]
          {::db/result added-timeslot})))

    (catch Exception exn
      (mulog/log ::failed-append-timeslot!
                 :exn (-> exn Throwable->map (dissoc :trace))
                 :timeslot-arg timeslot
                 :latest-timeslot-arg latest-timeslot)
      ; rethrows to let next.jdbc handle rollback of transaction, if any
      (throw exn))))


(defn add-timeslot! [db-conn {:as timeslot, :keys [duration-mins], :or {duration-mins 45}}]
  (try
    (let [{:as added-timeslot} (if-some [latest-timeslot (last-timeslot db-conn)]
                                 (-append-timeslot! db-conn latest-timeslot timeslot)
                                 (-add-first-timeslot! db-conn timeslot))]
      (update-in added-timeslot
                 [::db/result :start-at]
                 (fn [start-at-datetime]
                   (when (some? start-at-datetime)
                     (-> start-at-datetime
                         (.toLocalTime)
                         -stringify-start-at-time)))))

    (catch Exception exn
      (mulog/log ::failed-append-timeslot!
                 :exn (-> exn Throwable->map (dissoc :trace))
                 :timeslot-arg timeslot)
      ; rethrows to let next.jdbc handle rollback of transaction, if any
      (throw exn))))
