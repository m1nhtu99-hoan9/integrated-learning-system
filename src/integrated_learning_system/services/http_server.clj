(ns integrated-learning-system.services.http-server
  (:require
    [clojure.spec.alpha :as s]
    [com.brunobonacci.mulog :as mulog]
    [integrant.core :as ig]
    [io.pedestal.http :as pedestal-http]
    [reitit.pedestal]
    [integrated-learning-system.routing :refer [create-routing-interceptor]] ))

(s/def ::log-event #{::init-failed ::init-successfully})

(defn- create-server [pedestal-config-map app-config-map]
  (-> pedestal-config-map
      (pedestal-http/default-interceptors)
      ; plug-in the reitit-based routing interceptor
      (reitit.pedestal/replace-last-interceptor (create-routing-interceptor app-config-map))
      (pedestal-http/dev-interceptors)
      (pedestal-http/create-server)))

(defmethod ig/init-key :server/http
  [_ {:keys [pedestal app]}]
  (try
    (if-some [server-service-map (-> (create-server pedestal app) (pedestal-http/start))]
      (do (mulog/log ::init-successfully :instance server-service-map)
          server-service-map)
      (do (mulog/log ::init-failed :instance nil)
          nil))
    (catch Exception exn
      (mulog/log ::init-failed :exception exn)
      (throw exn))))

(defmethod ig/halt-key! :server/http
  [_ server]
  (pedestal-http/stop server))
