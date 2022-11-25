(ns integrated-learning-system.handlers.classes.homeworks
  (:require
    [com.brunobonacci.mulog :as mulog]
    [integrated-learning-system.db.homeworks :as homeworks-db]
    [integrated-learning-system.db.multiple-choice-questions :as mcq-db]
    [integrated-learning-system.db.schoolwork-questions :as swkq-db]
    [integrated-learning-system.handlers.classes.homework.commons :as commons]
    [integrated-learning-system.handlers.commons.api :as api]
    [integrated-learning-system.specs :refer [spec-explanation->validation-result]]
    [integrated-learning-system.specs.requests.classes :as s-classes]
    [integrated-learning-system.utils.datetime :as dt]
    [integrated-learning-system.utils.throwable :refer [exn->map]]
    [java-time.api :as jt]
    [next.jdbc :as jdbc])
  (:import [java.util Date]))

;;region PUT handlers

(defn- -validate-class-period-exists [db-conn {:as argmap, :keys [class-name]}]
  (let [[status-code result] (commons/identify-class-period db-conn argmap)]
    (case status-code
      200 result,
      422 {:non-ok-response (api/resp-422 (:title result)
                                          (:errors result))},
      500 {:non-ok-response (api/resp-500 (str "Failed to process this request adding homework for '" class-name "'.")
                                          nil)})))

(defn- -process-creating-homework [db-conn
                                   {:as path-params, :keys [class-name date slot-no]}
                                   {:keys [class-id course-id class-period-id]}
                                   multiple-choice-questions]
  (try
    (jdbc/with-transaction
      [db-tx db-conn]
      (comment
        {:step-1 "adding multiple-choice questions, get question-ids"
         :step-2 "adding homework, get schoolwork-id"
         :step-3 "link added homework with added question-ids by adding schoolwork-questions"})
      (let [added-question-ids (for [question multiple-choice-questions
                                     :let [question-title (:question-title question),
                                           question-body (dissoc question :question-title)]]
                                 (->> {:question-title question-title
                                       :question-body  question-body}
                                      (mcq-db/add-multiple-choice-question! db-tx)
                                      (:question-id))),
            added-question-ids (into [] added-question-ids),
            {added-schoolwork-id :schoolwork-id} (homeworks-db/add-homework db-tx {:due-class-period-id class-period-id
                                                                                   :course-id           course-id}),
            _ (swkq-db/add-with-schoolwork-id-and-question-ids! db-tx {:schoolwork-id added-schoolwork-id
                                                                       :question-ids  added-question-ids}),
            added-questions (mcq-db/multiple-choice-questions-by-due-class-period-id db-tx
                                                                                     {:due-class-period-id class-period-id}),
            added-questions (into [] added-questions)]
        (api/resp-201 (str "/api/v1/classes/" class-name "/periods/" (jt/format "uuuu-MM-dd") "/" slot-no "/homework")
                      added-questions)))

    (catch Exception exn
      (mulog/log ::failed-process-creating-homework
                 :exn (exn->map exn (fn [stack]
                                      (->> stack (take 8) (into []))))
                 :path-params path-params
                 :class-id class-id
                 :course-id course-id
                 :class-period-id class-period-id)
      (api/resp-500 (str "Failed to process this request uploading multiple choice questions as homework for class '"
                         class-name "'.")
                    nil))))

(defn create-homework
  "Handle creating homework. Current assumption: Request contains multiple-choice questions only."
  [{:as                                               req, :keys [body-params coercion-problems]
    {:keys [db-conn]}                                 :services
    {:keys [multiple-choice-questions]}               :body-params
    {{:keys [class-name, ^Date date, slot-no]} :path} :parameters}]
  (try
    (cond
      (some? coercion-problems) (api/resp-401 "Invalid request"
                                              (spec-explanation->validation-result s-classes/validation-messages
                                                                                   coercion-problems)),
      (nil? db-conn) (api/resp-302 "/api/ping"),
      :else
      (let [school-date (dt/->local-date date),
            path-params {:class-name      class-name
                         :school-date     school-date
                         :timeslot-number slot-no},
            {:as entity-ids, :keys [non-ok-response]} (-validate-class-period-exists db-conn path-params)]
        (if (some? non-ok-response)
          non-ok-response
          ; else: process adding homework questions
          (-process-creating-homework db-conn path-params entity-ids multiple-choice-questions))))

    (catch Exception exn
      (mulog/log ::failed-create-homework
                 :exn (exn->map exn (fn [stack]
                                      (->> stack (take 8) (into []))))
                 :path-params (:path-params req)
                 :body-params body-params
                 :coercion-problems coercion-problems)
      (api/resp-500 (str "Failed to process this request adding homework for '" class-name "'.")
                    nil))))

;;endregion
