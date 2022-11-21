(ns integrated-learning-system.handlers.webpages.classes
  (:require
    [integrated-learning-system.db.classes :as classes-db]
    [integrated-learning-system.db.timeslots :as timeslots-db]
    [integrated-learning-system.handlers.commons.api :as api]
    [integrated-learning-system.handlers.commons.html :refer [resp-200 resp-400 resp-501]]
    [integrated-learning-system.specs :refer [spec-explanation->validation-result]]
    [integrated-learning-system.specs.requests.classes :as s-classes]
    [integrated-learning-system.views.templates.classes :as classes-tmpl]))


;region action pages

(defn serve-organise-schedule-page [{:keys                        [coercion-problems services],
                                     {{:keys [class-name]} :path} :parameters}]
  (if (some? coercion-problems)
    (resp-400
      (classes-tmpl/organise-schedule
        {:path-errors (spec-explanation->validation-result s-classes/validation-messages
                                                           coercion-problems)}))
    (let [db-conn (:db-conn services),
          {:as this-class, :keys [class-id]} (some-> db-conn (classes-db/class-by-class-name {:class-name class-name})),
          class-periods-num (some-> db-conn (classes-db/count-class-periods {:class-id class-id}))]
      (cond
        (nil? db-conn) (api/resp-302 "/api/ping")
        (nil? this-class) (resp-400
                            (classes-tmpl/organise-schedule
                              {:path-errors {:class-name [(str "Class with name '" class-name "' did not exist.")]}})),
        (pos-int? class-periods-num) (resp-501 (classes-tmpl/organise-schedule {:class-name        class-name
                                                                                :class-periods-num class-periods-num})),
        :else (resp-200
                (classes-tmpl/organise-schedule
                  {:class-name class-name
                   :timeslots  (timeslots-db/all-timeslots db-conn)}))))))


(defn serve-manage-class-members-page [{:keys                        [coercion-problems]
                                        {:keys [db-conn]}            :services
                                        {{:keys [class-name]} :path} :parameters}]
  (if (some? coercion-problems)
    (resp-400
      (classes-tmpl/manage-class-members
        {:path-errors (spec-explanation->validation-result s-classes/validation-messages
                                                           coercion-problems)}))
    (let [db-query-params {:class-name class-name}
          {:as this-class, :keys [class-id]} (some-> db-conn (classes-db/class-by-class-name db-query-params))]
      (cond
        (nil? db-conn) (api/resp-302 "/api/ping")
        (nil? this-class) (resp-400
                            (classes-tmpl/manage-class-members
                              {:path-errors {:class-name [(str "Class with name '" class-name "' did not exist.")]}}))
        :else (resp-200 (classes-tmpl/manage-class-members {:class-name class-name}))))))

;endregion