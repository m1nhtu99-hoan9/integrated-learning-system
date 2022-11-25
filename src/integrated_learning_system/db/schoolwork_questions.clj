(ns integrated-learning-system.db.schoolwork-questions
  (:require
    [clojure.spec.alpha :as s]
    [com.brunobonacci.mulog :as mulog]
    [hugsql.core :as hugsql]
    [integrated-learning-system.db :as db]
    [integrated-learning-system.db.sql.commons :refer [path-to-sql]]
    [integrated-learning-system.utils.throwable :refer [exn->map]]
    [next.jdbc.sql :as sql])
  (:import (java.util UUID)))


(comment
  (hugsql/def-db-fns (path-to-sql "schoolwork_questions")))


(defn add-with-schoolwork-id-and-question-ids! [db-conn {:as argmap, :keys [schoolwork-id question-ids]}]
  (cond
    (->> schoolwork-id
         (instance? UUID)
         not) (do
                (mulog/log ::add-with-question-ids-and-schoolwork-id!-invalid-arg
                           :schoolwork-id-arg schoolwork-id)
                (throw (IllegalArgumentException.
                         (str "schoolwork-id is not an UUID: " schoolwork-id)))),
    (->> question-ids
         (s/valid? (s/coll-of #(instance? UUID %)))
         not) (do
                (mulog/log ::add-with-question-ids-and-schoolwork-id!-invalid-arg
                           :question-ids-arg question-ids)
                (throw (IllegalArgumentException.
                         (str "question-ids is not a collection of UUID: " schoolwork-id)))),
    :else
    (try
      (sql/insert-multi! (db/with-snake-kebab-opts db-conn)
                         :schoolwork-question
                         [:schoolwork-id :question-id]
                         (for [question-id question-ids]
                           [schoolwork-id question-id]))

      (catch Exception exn
        (mulog/log ::failed-add-with-schoolwork-id-and-question-ids
                   :args argmap
                   :exn (exn->map exn (fn [stack]
                                         (->> stack (take 12) (into [])))))
        ; rethrows to let next.jdbc handle rollback of transaction, if any
        (throw exn)))))
