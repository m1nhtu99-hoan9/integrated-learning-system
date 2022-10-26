(ns integrated-learning-system.routing
  (:require
    [camel-snake-kebab.core :as csk]
    [integrated-learning-system.handlers.commons :refer [handle-ping-fn]]
    [integrated-learning-system.routing.students :refer [create-student-routes]]
    [integrated-learning-system.specs.server :as s-server]
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

(def router-opts
  (let [muuntaja-opts (-> muuntaja/default-options
                          (muuntaja/select-formats ["application/json"])
                          (assoc-in [:formats "application/json" :encoder-opts]
                                    ; jsonista opts
                                    {:encode-key-fn csk/->camelCaseString})
                          (assoc-in [:formats "application/json" :decoder-opts]
                                    ; jsonista opts
                                    {:decode-key-fn csk/->kebab-case-keyword})),
        muuntaja-instance (muuntaja/create muuntaja-opts)]
    {:exception reitit-pretty/exception
     :data      {:coercion     coercion-instance
                 :muuntaja     muuntaja-instance
                 :interceptors [swagger-feature
                                (format-negotiate-interceptor muuntaja-instance)
                                (format-response-interceptor muuntaja-instance) (exception-interceptor)
                                (format-request-interceptor muuntaja-instance)]}}))

(defn- create-swagger-docs [{:keys [name version]}]
  ["/swagger.json" {:get {:no-doc  true
                          :swagger {:basePath "/"
                                    :info     {:title       name
                                               :description (str name " API Reference")
                                               :version     version}}
                          :handler (create-swagger-handler)}}])

(defn create-router [app-config]
  (http/router
    [(create-swagger-docs app-config)
     ["/ping" {:get {:summary   "Health check"
                     :responses {200 {:body {:message string?}}}
                     :handler   (handle-ping-fn app-config)}}]
     ["/api"
      ["/v1"
       [["/students" (create-student-routes)]]]]]
    router-opts))

(defn create-default-handler []
  "Create fallback `ring` handler for when no matched routes found"
  (ring/routes
    (create-swagger-ui-handler {:path "/"})
    (ring/create-resource-handler)
    (ring/create-default-handler)))
