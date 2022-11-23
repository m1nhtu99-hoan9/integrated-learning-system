(ns integrated-learning-system.routing
  (:require
    [camel-snake-kebab.core :as csk]
    [integrated-learning-system.handlers.commons :refer [handle-api-ping-fn redirect-to-page-404]]
    [integrated-learning-system.interceptors.http :refer [safe-coerce-request-interceptor]]
    [integrated-learning-system.routing.api :refer [api-v1-routes]]
    [integrated-learning-system.routing.pages :refer [webpage-routes]]
    [muuntaja.core :as muuntaja]
    [reitit.coercion.spec :refer [coercion] :rename {coercion coercion-instance}]
    [reitit.dev.pretty :as reitit-pretty]
    [reitit.http :as http]
    [reitit.http.interceptors.exception :refer [exception-interceptor]]
    [reitit.http.interceptors.multipart :refer [multipart-interceptor]]
    [reitit.http.interceptors.muuntaja :refer [format-negotiate-interceptor
                                               format-request-interceptor
                                               format-response-interceptor]]
    [reitit.http.interceptors.parameters :refer [parameters-interceptor]]
    [reitit.pedestal]
    [reitit.ring :as ring]
    [reitit.ring.spec :as rrs]
    [reitit.spec :as rs]
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
                      ;; for processing query-params & form-params
                      (parameters-interceptor)
                      ;; for processing header content negotiation
                      (format-negotiate-interceptor muuntaja-instance)
                      ;; for response body serialisation
                      (format-response-interceptor)
                      ;; for exception handling (only under prod env)
                      :exception-interceptor
                      ;; for request body deserialisation
                      (format-request-interceptor muuntaja-instance)
                      ;; for coercing request params
                      (safe-coerce-request-interceptor)
                      ;; for handling HTTP multipart request
                      (multipart-interceptor)]]
    {:exception   reitit-pretty/exception
     :validate    rrs/validate
     ::rs/explain expound.alpha/expound-str
     :data        {:coercion     coercion-instance
                   :muuntaja     muuntaja-instance
                   :interceptors (replace {:exception-interceptor (when (= env :prod)
                                                                    (exception-interceptor))}
                                          interceptors)}}))


(defn- create-swagger-docs [{:keys [name version]}]
  ["/swagger.json" {:get {:no-doc  true
                          :swagger {:basePath "/"
                                    :info     {:title       name
                                               :description (str name " API Reference")
                                               :version     version}}
                          :handler (create-swagger-handler)}}])

(defn create-router
  "The reitit.http/router powers the app's primary routes."
  [app-config]
  (http/router
    [["/api/ping" {:get     {:summary   "Health check"
                             :responses {200 {:body {:message string?}}}
                             :handler   (handle-api-ping-fn app-config)}
                   :swagger {:tags ["general"]}}]
     (api-v1-routes)
     (webpage-routes)
     (create-swagger-docs app-config)]
    (router-opts app-config)))

(defn create-default-handler
  "ring composite route handler responsible for routing for swagger-ui, static resource & HTTP 404 request."
  []
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
