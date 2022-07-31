(ns integrated-learning-system.routing
  (:require
    [clojure.spec.alpha :as s]
    [integrated-learning-system.routing.students :refer [create-student-routes]]
    [integrated-learning-system.services.app :as app-service]
    [io.pedestal.http.route :as route]
    [muuntaja.core :as muuntaja]
    [reitit.coercion.spec :refer [coercion] :rename {coercion coercion-instance}]
    [reitit.dev.pretty :as reitit-pretty]
    [reitit.http :as http]
    [reitit.http.interceptors.exception :refer [exception-interceptor]]
    [reitit.http.interceptors.muuntaja :refer [format-request-interceptor
                                               format-response-interceptor
                                               format-negotiate-interceptor]]
    [reitit.pedestal]
    [reitit.ring :as ring]
    [reitit.swagger :refer [create-swagger-handler swagger-feature]]
    [reitit.swagger-ui :refer [create-swagger-ui-handler]]))

(defonce router-opts
         {:exception reitit-pretty/exception
          :data      {:coercion     coercion-instance
                      :muuntaja     muuntaja/instance
                      :interceptors [swagger-feature
                                     exception-interceptor
                                     format-request-interceptor
                                     format-response-interceptor
                                     format-negotiate-interceptor]}})

(defn- create-swagger-docs [{:keys [name version]}]
  ["/swagger.json" {:get {:no-doc  true
                          :swagger {:basePath "/"
                                    :info     {:title       name
                                               :description (str name " API Reference")
                                               :version     version}}
                          :handler (create-swagger-handler)}}])

(defn- create-router [app-config]
  (http/router
    [(create-swagger-docs app-config)
     ["/ping" (constantly {:status 200 :body "Running at v1"})]
     ["/v1" [{:swagger {:summary "API v1"}}
             ["/students" (create-student-routes)]]]]
    router-opts))

(defn- create-default-handler []
  "Create ring handle for handling when no matched routes found"
  (ring/routes
    (create-swagger-ui-handler {:path   "/"
                                :config {:validatorUrl nil}})
    (ring/create-resource-handler)
    (ring/create-default-handler)))

(defn create-routing-interceptor [app-config]
  "Create `pedestal` interceptor for `reitit`-based routing"
  {:pre (s/valid? ::app-service/config-map app-config)}

  (reitit.pedestal/routing-interceptor (create-router app-config)
                                       create-default-handler))

(comment
  ;; io.pedestal.http.route experiment
  (defn- handle-ping [request]
    {:status 200, :body "Hello World"})
  (defn- handle-get-students [request]
    {:status 200, :body "Acknowledged GET request"})
  (defn- handle-upsert-students [request]
    {:status 200, :body "Acknowledged POST request"})

  (defonce ^:private routes-spec
           #{{:app-name "Online Learning System", :schema :http, :host "online-learning-system.org"} ; explicit syntax
             ; terse syntax
             ["/ping" {:get `handle-ping}]
             ["/students" {:get  `handle-get-students,
                           :post `handle-upsert-students}
              ["/:student-id" {:put `handle-upsert-students}]]})
  (defonce routes (route/expand-routes routes-spec)))