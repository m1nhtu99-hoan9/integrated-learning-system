(ns integrated-learning-system.db.multiple-choice-questions
  (:require
    [com.brunobonacci.mulog :as mulog]
    [hugsql.core :as hugsql]
    [integrated-learning-system.db.sql.commons :refer [->pgsql-jsonb path-to-sql]]
    [integrated-learning-system.utils.json :refer [read-json]]
    [integrated-learning-system.utils.throwable :refer [exn->map]]))


(defn -add-multiple-choice-question! [db-conn {:keys [question_title question_json_body]}]
  (comment "hugsql will re-define this fn."))
(defn -multiple-choice-questions-by-due-class-period-id [db-conn {:keys [due_class_period_id]}]
  (comment "hugsql will re-define this fn."))
(hugsql/def-db-fns (path-to-sql "multiple_choice_questions"))


(defn add-multiple-choice-question! [db-conn {:as argmap, :keys [question-title question-body]}]
  (try
    (-add-multiple-choice-question! db-conn
                                    {:question_title question-title
                                     :question_json_body (->pgsql-jsonb question-body)})
    (catch Exception exn
      (mulog/log ::failed-add-multiple-choice-question
                 :exn (exn->map exn (fn [stack]
                                      (->> stack (take 8) (into []))))
                 :args argmap)
      ; rethrows to let next.jdbc handle rollback of transaction, if any
      (throw exn))))


(defn multiple-choice-questions-by-due-class-period-id
  ([db-conn
    {:as params, :keys [due-class-period-id]}
    {:as opts
     :keys [question-id? schoolwork-id?]
     :or {question-id? false
          schoolwork-id? false}}]
   (try
     (let [results (-multiple-choice-questions-by-due-class-period-id db-conn {:due_class_period_id due-class-period-id})]
       (for [{:keys [question-title question-body question-id schoolwork-id]} results
             :let [question (read-json (.toString question-body)),
                   question (assoc question :question-title question-title),
                   question (if question-id?
                              (assoc question :question-id question-id)
                              question),
                   question (if schoolwork-id?
                              (assoc question :schoolwork-id schoolwork-id)
                              question)]]
         question))
     (catch Exception exn
       (mulog/log ::failed-add-multiple-choice-question
                  :exn (exn->map exn (fn [stack]
                                       (->> stack (take 15) (into []))))
                  :args {:params params
                         :opts opts})
       ; rethrows to let next.jdbc handle rollback of transaction, if any
       (throw exn))))
  ([db-conn params]
   (multiple-choice-questions-by-due-class-period-id db-conn params {})))
