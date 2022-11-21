(ns integrated-learning-system.db.teacher-classes
  (:require
    [com.brunobonacci.mulog :as mulog]
    [hugsql.core :as hugsql]
    [integrated-learning-system.db.sql.commons :refer [path-to-sql]]
    [integrated-learning-system.utils.throwable :refer [exn->map]])
  (:import [java.util UUID]))


(defn -insert-teacher-class-if-not-exists [db-conn {:keys [teacher_id class_id]}]
  (comment "hugsql will re-define this fn"))
(defn -delete-teacher-classes-by-class-name [db-conn {:keys [class_name]}]
  (comment "hugsql will re-define this fn"))
(hugsql/def-db-fns (path-to-sql "teacher_classes"))


(defn safe-insert-teacher-class! [db-conn {:as argmap, :keys [teacher-id class-id]}]
  (cond
    (not (instance? UUID teacher-id)) (throw (IllegalArgumentException.
                                               (str "teacher-id not a valid UUID: " teacher-id))),
    (not (instance? UUID class-id)) (throw (IllegalArgumentException.
                                             (str "class-id not a valid UUID: " class-id))),
    :else (try
            (-insert-teacher-class-if-not-exists db-conn {:teacher_id teacher-id
                                                          :class_id   class-id})
            (catch Exception exn
              (mulog/log ::failed-safe-insert-teacher-class!
                         :args argmap
                         :exn (exn->map exn (fn [stack]
                                              (->> stack (take 8) (into [])))))
              ; rethrows to let next.jdbc handle rollback of transaction, if any
              (throw exn)))))


(defn delete-teacher-classes-by-class-name! [db-conn {:keys [class-name]}]
  (try
    (-delete-teacher-classes-by-class-name db-conn {:class_name class-name})
    (catch Exception exn
      (mulog/log ::failed-delete-teacher-classes-by-class-name!
                 :class-name-arg class-name
                 :exn (exn->map exn (fn [stack]
                                      (->> stack (take 8) (into [])))))
      ; rethrows to let next.jdbc handle rollback of transaction, if any
      (throw exn))))
