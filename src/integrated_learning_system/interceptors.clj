(ns integrated-learning-system.interceptors
  (:require
    [io.pedestal.interceptor :refer [interceptor]]
    [integrated-learning-system.routing :as routing]
    [integrated-learning-system.db :refer [halt-db-conn init-db-conn]]))

;region db-conn

(defn -fn-register-db-conn [ctx postgres-cfgmap]
  ;; ctx: global context managed by pedestal
  ;; postgres-cfgmap: database configuration read from the config file,
  ;;                  either a hash-map understood by next.jdbc or a javax.sql.DataSource object
  (if-some [db-conn (init-db-conn postgres-cfgmap)]
    ; non-null -> includes the initiated `db-conn` to the service collection
    (update-in ctx [:request :services]
               merge {:db-conn db-conn})
    ; null -> persists the failure message
    (update-in ctx [:request :failures]
               merge {:database "Failed to establish connection with database."})))

(defn -fn-unregister-db-conn [ctx]
  (as-> ctx $
        (update-in $ [:request :services]
                   (fn [service-map]
                     (when-some [conn (:db-conn service-map)]
                       (halt-db-conn conn))
                     ; nothing would happen when `:db-conn` key is not in the map
                     (dissoc service-map :db-conn)))
        (update-in $ [:request :failures]
                   (fn [failure-map]
                     (comment "cleanup failure message(s) if written.")
                     (dissoc failure-map :database)))))

(defn create-db-conn-interceptor [postgres-cfgmap]
  "Create an interceptor for providing new database connection
  on enter and close the connection (if opened) on leave"

  (interceptor
    {:name  ::db-conn
     :enter #(-fn-register-db-conn % postgres-cfgmap)
     :leave -fn-unregister-db-conn}))

;endregion

;region routing

(defn create-routing-interceptor [app-config]
  "Create `pedestal` interceptor for `reitit`-based routing"

  (reitit.pedestal/routing-interceptor (routing/create-router app-config)
                                       (routing/create-default-handler)))

;endregion
