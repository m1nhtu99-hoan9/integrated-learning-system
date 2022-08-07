(ns integrated-learning-system.interceptors
  (:require
    [io.pedestal.interceptor :refer [interceptor]]
    [integrated-learning-system.routing :as routing]
    [integrated-learning-system.services.db :refer [halt-db-conn init-db-conn]]))

(defn db-conn-interceptor [postgres-cfgmap]
  "Create interceptor for creating new database connection on enter and close the connection (if opened) on leave"

  (interceptor
    {:name  ::db-conn
     :enter (fn [ctx]
              (update-in ctx [:request :services]
                         merge {:db-conn (init-db-conn postgres-cfgmap)}))
     :leave (fn [ctx]
              (get-in ctx [:request :services])
              (update-in ctx [:request :services]
                         #(let [db-conn (:db-conn %)]
                            (halt-db-conn db-conn)
                            (dissoc % :db-conn))))}))

(defn routing-interceptor [app-config]
  "Create `pedestal` interceptor for `reitit`-based routing"

  (reitit.pedestal/routing-interceptor (routing/create-router app-config)
                                       (routing/create-default-handler)))
