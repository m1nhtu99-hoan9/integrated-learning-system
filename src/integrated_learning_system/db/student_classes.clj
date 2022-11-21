(ns integrated-learning-system.db.student-classes
  (:require
    [clojure.spec.alpha :as s]
    [com.brunobonacci.mulog :as mulog]
    [hugsql.core :as hugsql]
    [integrated-learning-system.db.sql.commons :refer [path-to-sql]]
    [integrated-learning-system.utils.throwable :refer [exn->map]])
  (:import [java.util UUID]))


(defn -insert-student-class-if-not-exists [db-conn {:keys [student_id class_id]}]
  (comment "hugsql will re-define this fn"))
(defn -insert-student-classes-if-not-exists [db-conn {:keys [student_ids_and_class_ids]}]
  (comment "hugsql will re-define this fn"))
(defn -delete-student-classes-by-class-name [db-conn {:keys [class_name]}]
  (comment "hugsql will re-define this fn"))

(hugsql/def-db-fns (path-to-sql "student_classes"))


(defn safe-insert-student-class! [db-conn {:keys [student-id class-id]}]
  (-insert-student-class-if-not-exists db-conn {:student_id student-id
                                                :class_id   class-id}))

(defn safe-insert-student-classes! [db-conn {:as argmap, :keys [student-classes]}]
  (let [student-ids-and-class-ids (map #(vector (:student-id %) (:class-id %)) student-classes)]
    (try
      (-insert-student-classes-if-not-exists db-conn {:student_ids_and_class_ids student-ids-and-class-ids})

      (catch Exception exn
        (mulog/log ::failed-safe-insert-student-classes
                   :args argmap
                   :student-ids-and-class-ids student-ids-and-class-ids
                   :exn (exn->map exn (fn [stack]
                                        (->> stack (take 8) (into [])))))
        ; rethrows to let next.jdbc handle rollback of transaction, if any
        (throw exn)))))


(defn insert-students-to-class! [db-conn {:as argmap, :keys [student-ids class-id]}]
  (cond
    (not (instance? UUID class-id)) (throw (IllegalArgumentException.
                                             (str "class-id not a valid UUID: " class-id))),
    (->> student-ids
         (s/valid? (s/coll-of #(instance? UUID %)))
         not) (do (mulog/log ::insert-students-to-class!-invalid-args
                             :student-ids student-ids)
                  (throw (IllegalArgumentException. (str "student-ids not a collection of UUID.")))),
    :else (try
            (safe-insert-student-classes! db-conn
                                          {:student-classes (for [student-id student-ids]
                                                              {:student-id student-id
                                                               :class-id class-id})})
            (catch Exception exn
              (mulog/log ::failed-insert-students-to-class!
                         :args argmap
                         :exn (exn->map exn (fn [stack]
                                              (->> stack (take 8) (into [])))))
              ; rethrows to let next.jdbc handle rollback of transaction, if any
              (throw exn)))))


(defn delete-student-classes-by-class-name! [db-conn {:keys [class-name]}]
  (try
    (-delete-student-classes-by-class-name db-conn {:class_name class-name})

    (catch Exception exn
      (mulog/log ::failed-delete-student-classes-by-class-name!
                 :class-name-arg class-name
                 :exn (exn->map exn (fn [stack]
                                      (->> stack (take 8) (into [])))))
      ; rethrows to let next.jdbc handle rollback of transaction, if any
      (throw exn))))
