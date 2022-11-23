(ns user
  "This namespace gets loaded the first"
  (:require
    [integrant.core :refer [load-namespaces] :as ig]
    [integrant.repl :as ig-repl]
    [integrated-learning-system.server :refer [config-fname->map start-console-log-publisher!]]))
(require '[integrated-learning-system.db :as db])
(require '[integrated-learning-system.utils.datetime :as dt])
(require '[integrated-learning-system.routing :as routing])
(require '[java-time.api :as jt])
(require '[integrant.repl.state :as ig-state])
(require '[reitit.core :as reitit])
(require '[reitit.ring :as ring])
(require '[user.data-seeding :as seeding])

(defonce ^:private config-fname "config_dev.edn")

(start-console-log-publisher!)

(ig-repl/set-prep!
  (constantly (-> config-fname config-fname->map ig/prep)))

(defn start-dev []
  (ig-repl/go)
  :started)

(defn stop-dev []
  (ig-repl/halt)
  :stopped)

(defn restart-dev []
  (stop-dev)
  (start-dev)
  :restarted)


(comment
  ; (resultset-seq)

  (load-namespaces ig-state/config)
  ig-state/system

  (restart-dev)
  (stop-dev)
  (start-dev)

  ; smoke testing: router
  (def router
    (let [app-cfg (:server/app ig-state/system)]
      (routing/create-router app-cfg)))
  (reitit/match-by-path router "/api/v1/accounts/admin")
  ;  inspect route definitions resolved by reitit
  (reitit/routes router)

  ; smoke testing: routing-handler
  (def routing-handler
   (ring/ring-handler router
                      (routing/create-default-handler)))
  ;; Without the muuntaja 'format-middleware', opening this URI through web browser would lead to
  ;; HTTP ERROR 500 java.lang.IllegalArgumentException:
  ;; No implementation of method: :write-body-to-stream of protocol: #'ring.core.protocols/StreamableResponseBody found
  ;; for class: clojure.lang.PersistentArrayMap
  (routing-handler {:request-method :get
                    :uri            "/"}))
