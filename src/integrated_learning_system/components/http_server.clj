(ns integrated-learning-system.components.http-server
  (:require
    [clojure.spec.alpha :as s]
    [com.brunobonacci.mulog :as mulog]
    [io.pedestal.http :as pedestal-http]
    [reitit.pedestal]
    [integrated-learning-system.routing :refer [create-routing-interceptor]])
  (:import [com.stuartsierra.component Lifecycle]))

(s/def ::log-event #{::init-failed ::init-successfully ::closed ::close-failed})

(defn- pedestal-server [pedestal-cfgmap app-cfgmap]
  (let [routing-interceptor (create-routing-interceptor app-cfgmap)]
    (-> pedestal-cfgmap
        (pedestal-http/default-interceptors)
        ; plug in the reitit-based routing interceptor
        (reitit.pedestal/replace-last-interceptor routing-interceptor)
        (pedestal-http/dev-interceptors)
        (pedestal-http/create-server))))

(defrecord HttpServerComponent [server-cfgmap http-server db-conn]
  Lifecycle

  (start [this]
    (let [{pedestal-cfgmap :http, app-cfgmap :app} server-cfgmap
          {app-name :name, app-ver :version, app-env :env} app-cfgmap]
      (try
        (if-some [pedestal-server (-> (pedestal-server pedestal-cfgmap app-cfgmap)
                                      (pedestal-http/start))]
          (do
            (mulog/log ::init-successfully :instance pedestal-server)
            (mulog/set-global-context! {:app-name app-name, :version app-ver, :env (name app-env)})
            (assoc this :http-server pedestal-server))
          (do
            (mulog/log ::init-failed :instance nil)
            this))
        (catch Exception exn
          (mulog/log ::init-failed :exception exn)
          (throw exn)))))

  (stop [this]
    (if (some? http-server)
      (try
        (pedestal-http/stop http-server)
        (mulog/log ::closed)
        (assoc this :http-server nil)
        (catch Exception exn
          (mulog/log ::close-failed :exception exn)
          (throw exn))))))

(defn http-server-component [server-cfgmap]
  (HttpServerComponent. server-cfgmap nil nil))