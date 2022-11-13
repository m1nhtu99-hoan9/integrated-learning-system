(ns integrated-learning-system.routing
  (:require
    [camel-snake-kebab.core :as csk]
    [integrated-learning-system.handlers.commons :refer [handle-api-ping-fn redirect-to-page-404]]
    [integrated-learning-system.routing.api :refer [api-v1-routes]]
    [integrated-learning-system.routing.pages :refer [webpage-routes]]
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


(defn router-opts [{:keys [env]}]
  (let [muuntaja-opts (-> muuntaja/default-options
                          (muuntaja/select-formats ["application/json"])
                          (assoc-in [:formats "application/json" :encoder-opts]
                                    ; jsonista opts
                                    {:encode-key-fn csk/->camelCaseString})
                          (assoc-in [:formats "application/json" :decoder-opts]
                                    ; jsonista opts
                                    {:decode-key-fn csk/->kebab-case-keyword})),
        muuntaja-instance (muuntaja/create muuntaja-opts),
        interceptors [swagger-feature
                      (format-negotiate-interceptor muuntaja-instance)
                      (format-response-interceptor muuntaja-instance)
                      (format-request-interceptor muuntaja-instance)]]
    {:exception reitit-pretty/exception
     :data      {:coercion     coercion-instance
                 :muuntaja     muuntaja-instance
                 :interceptors (if (= env :prod)
                                 (conj interceptors exception-interceptor)
                                 interceptors)}}))


(defn- create-swagger-docs [{:keys [name version]}]
  ["/swagger.json" {:get {:no-doc  true
                          :swagger {:basePath "/"
                                    :info     {:title       name
                                               :description (str name " API Reference")
                                               :version     version}}
                          :handler (create-swagger-handler)}}])

(defn create-router [app-config]
  (http/router
    [["/api/ping" {:get     {:summary   "Health check"
                             :responses {200 {:body {:message string?}}}
                             :handler   (handle-api-ping-fn app-config)}
                   :swagger {:tags ["general"]}}]
     (api-v1-routes)
     (webpage-routes)
     (create-swagger-docs app-config)]
    (router-opts app-config)))

(defn create-default-handler []
  (ring/routes
    ; swagger-ui
    (create-swagger-ui-handler {:path "/swagger"})
    (comment
      ; handling trailing splash
      (ring/redirect-trailing-slash-handler {:method :strip}))
    ; handling static resources (under "resources/public" folder)
    (ring/create-resource-handler {:path "/" :root "public"})
    ; fallback handler for when no matched routes resolved
    (ring/create-default-handler {:not-found (constantly (redirect-to-page-404))})))
