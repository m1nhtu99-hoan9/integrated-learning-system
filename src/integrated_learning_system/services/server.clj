(ns integrated-learning-system.services.server
  (:require
    [clojure.spec.alpha :as s]
    [com.brunobonacci.mulog :as mulog]
    [io.pedestal.http :as pedestal-http]
    [integrant.core :as ig]
    [reitit.pedestal]
    [integrated-learning-system.interceptors :refer [db-conn-interceptor routing-interceptor]]
    [integrated-learning-system.specs.config.http :as s-http]
    [integrated-learning-system.specs.config.app :as s-app]))

(s/def ::log-event #{::init-failed ::init-successfully ::closed ::close-failed})

(defn- pedestal-server [pedestal-cfgmap {:as app-cfgmap, :keys [env]} postgres-cfgmap]
  (let [routing-interceptor (routing-interceptor app-cfgmap)
        db-conn-interceptor (db-conn-interceptor postgres-cfgmap)]
    (-> pedestal-cfgmap
        (pedestal-http/default-interceptors)
        ; plug in the reitit-based routing interceptor
        (reitit.pedestal/replace-last-interceptor routing-interceptor)
        ; the dev-interceptors only supplemented in dev env
        (cond->
          (= env :dev) (pedestal-http/dev-interceptors))
        ; plug in the db-conn-interceptor at the last
        (update-in [::pedestal-http/interceptors] conj db-conn-interceptor)
        (pedestal-http/create-server))))


;; :server/app

(defmethod ig/init-key :server/app
  [_ {:as app-cfgmap, app-name :name, app-ver :version, app-env :env}]

  (mulog/set-global-context! {:app-name app-name, :version app-ver, :env (name app-env)})
  app-cfgmap)

(defmethod ig/pre-init-spec :server/app
  [_]
  ::s-app/config-map)


;; :server/http

(defmethod ig/init-key :server/http
  [_ cfgmap]
  (let [pedestal-cfgmap (dissoc cfgmap :app-infos :db-infos)
        app-cfgmap (:app-infos cfgmap)
        postgres-cfgmap (:db-infos cfgmap)]
    (try
      (if-some [pedestal-server (-> (pedestal-server pedestal-cfgmap app-cfgmap postgres-cfgmap)
                                    (pedestal-http/start))]
        (do
          (mulog/log ::init-successfully)
          pedestal-server)
        (do
          (mulog/log ::init-failed)
          nil))

      (catch Exception exn
        (mulog/log ::init-failed :exception exn)
        (throw exn)))))

(defmethod ig/halt-key! :server/http
  [_ http-server]
  (if (some? http-server)
    (try
      (pedestal-http/stop http-server)
      (mulog/log ::closed)

      (catch Exception exn
        (mulog/log ::close-failed :exception exn)
        (throw exn)))))

(defmethod ig/pre-init-spec :server/http
  [_]
  ::s-http/config-map)
