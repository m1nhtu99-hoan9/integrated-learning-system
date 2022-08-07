(ns user
  (:require
    [integrant.core :refer [load-namespaces]]
    [integrant.repl :as ig-repl]
    [integrant.repl.state :as ig-state]
    [integrated-learning-system.server :as server :refer [config-fname->map start-console-log-publisher!]]))

(defonce ^:private config-fname "config_dev.edn")

(start-console-log-publisher!)

(ig-repl/set-prep!
  (constantly (-> config-fname config-fname->map)))

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

  (if-some [config (config-fname->map config-fname)]
    (let [db-component (-> config
                           (get-in [:db :postgres])
                           (database-component)
                           (start))
          db-conn (:db-conn db-component)]
      db-conn))

  (server/-main config-fname))

(comment
  ;; Without the muuntaja 'format-middleware', opening this URI through web browser would lead to
  ;; HTTP ERROR 500 java.lang.IllegalArgumentException:
  ;; No implementation of method: :write-body-to-stream of protocol: #'ring.core.protocols/StreamableResponseBody found
  ;; for class: clojure.lang.PersistentArrayMap
  (app {:request-method :get
        :uri            "/swagger.json"}))