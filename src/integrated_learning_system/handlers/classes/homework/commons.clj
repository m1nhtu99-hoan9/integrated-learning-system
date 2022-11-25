(ns integrated-learning-system.handlers.classes.homework.commons
  "Shared functions between API & SSR handler for processing/querying homework."
  (:require
    [com.brunobonacci.mulog :as mulog]
    [integrated-learning-system.db.classes :as classes-db]
    [integrated-learning-system.utils.throwable :refer [exn->map]]
    [java-time.api :as jt])
  (:import (java.time LocalDate)))


(defn identify-class-period [db-conn {:as argmap, :keys [class-name, ^LocalDate school-date, timeslot-number]}]
  (try
    (let [{:as this-class, :keys [class-id course-id]} (classes-db/class-by-class-name db-conn {:class-name class-name}),
          {:keys [class-period-id]} (when (some? this-class)
                                      (classes-db/class-class-period-at-date-by-class-id
                                        db-conn
                                        {:class-id        class-id
                                         :date            school-date
                                         :timeslot-number timeslot-number}))]
      (cond
        (nil? this-class) [422 {:title  "Unknown class"
                                :errors {:class-name [(str "Class named '" class-name "' does not exists.")]}}],
        (nil? class-period-id) [422 {:title  (str "Class '" class-name "': There's no class periods at "
                                                  (jt/format "dd/MM/uuuu" school-date) " timeslot " timeslot-number)
                                     :errors {}}],
        :else [200 {:class-id        class-id
                    :course-id       course-id
                    :class-period-id class-period-id}]))

    (catch Exception exn
      (mulog/log ::failed-identify-class-period
                 :exn (exn->map exn (fn [stack]
                                      (->> stack (take 8) (into []))))
                 :args argmap)
      [500 nil])))
