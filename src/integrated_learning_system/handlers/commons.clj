(ns integrated-learning-system.handlers.commons
  (:require [integrated-learning-system.handlers.commons.api :as api]))

(defn- failure->error
  "Map global failure entry into user-friendly error report"
  [[key msg]]
  {:area    key
   :message msg})

(defn handle-api-ping-fn [{:as app-cfgmap, :keys [env]}]
  (fn [req]
    (let [global-failures (:failures req)
          payload (if (empty? (:failures req))
                    {:message "Server is healthy.", :errors []}
                    {:message "Server is not ready for requests.", :errors (map failure->error
                                                                                global-failures)})
          debug-payload {:scheme           (:scheme req)
                         :request-body     (:body-params req)
                         :app-config       app-cfgmap
                         :contain-db-conn? (some? (get-in req [:services :db-conn]))}]
      (api/resp-200 (conj payload
                          (when (not= env :prod) {:debug-infos debug-payload}))))))

(defn redirect-to-page-404 []
  (api/resp-302 "/404"))


(defn user-display-names [{:keys [first-name last-name username]}]
  (let [full-name (str first-name " " last-name),
        display-name (str full-name " (" username ")")]
    {:full-name full-name, :display-name display-name}))
