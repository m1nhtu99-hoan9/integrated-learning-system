(ns integrated-learning-system.handlers.commons
  (:require [integrated-learning-system.handlers.commons.api :refer [resp-200]]
            [com.brunobonacci.mulog :as mulog]))

(defn- failure->error
  "Map global failure entry into user-friendly error report"
  [[key msg]]
  {:area key
   :message msg})

(defn handle-ping-fn [{:as app-cfgmap, :keys [env]}]
  (fn [req]
    (let [global-failures (:failures req)
          payload (if (empty? (:failures req))
                    {:message "Healthy", :errors []}
                    {:message "Unhealthy", :errors (map failure->error
                                                        global-failures)})
          debug-payload {:scheme           (:scheme req)
                         :request-body     (:body-params req)
                         :app-config       app-cfgmap
                         :contain-db-conn? (some? (get-in req [:services :db-conn]))}]
      (resp-200 (conj payload
                      (when (not= env :prod) {:debug-infos debug-payload}))))))
