(ns integrated-learning-system.routing
  (:require
    [integrated-learning-system.routing.students :refer [create-student-routes]]
    [muuntaja.core :as muuntaja]
    [reitit.coercion.spec :refer [coercion] :rename {coercion coercion-instance}]
    [reitit.dev.pretty :as reitit-pretty]
    [reitit.http :as http]
    [reitit.http.interceptors.exception :refer [exception-interceptor]]
    [reitit.http.interceptors.muuntaja :refer [format-negotiate-interceptor
                                               format-request-interceptor
                                               format-response-interceptor]]
    [reitit.pedestal]
    [reitit.ring :as ring]
    [reitit.swagger :refer [create-swagger-handler swagger-feature]]
    [reitit.swagger-ui :refer [create-swagger-ui-handler]]))

(defonce router-opts
  {:exception reitit-pretty/exception
   :data      {:coercion     coercion-instance
               :muuntaja     muuntaja/instance
               :interceptors [swagger-feature
                              (exception-interceptor)
                              (format-negotiate-interceptor)
                              (format-request-interceptor)
                              (format-response-interceptor)]}})

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
     ["/ping" {:get {:handler (constantly {:status 200 :body "Running at v1"})}}]
     ["/v1"
      [["/students" (create-student-routes)]]]]
    router-opts))

(defn- create-default-handler []
  "Create ring handle for handling when no matched routes found"
  (ring/routes
    (create-swagger-ui-handler {:path "/"})
    (ring/create-resource-handler)
    (ring/create-default-handler)))

(defn create-routing-interceptor [app-config]
  "Create `pedestal` interceptor for `reitit`-based routing"

  (reitit.pedestal/routing-interceptor (create-router app-config)
                                       (create-default-handler)))