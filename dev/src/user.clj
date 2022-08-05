(ns user
  (:require
    [com.brunobonacci.mulog :as mulog]
    [com.brunobonacci.mulog.core :refer [publishers]]
    [com.stuartsierra.component.repl :as c-repl]
    [integrated-learning-system.components.database :refer [database-component]]
    [integrated-learning-system.server :as server :refer [config-fname->map create-system]])
  (:import (com.brunobonacci.mulog.publisher ConsolePublisher)))

(defonce ^:private config-fname "config_dev.edn")

(defn mk-system [_]
  (when (not-any? #(instance? ConsolePublisher %) @publishers)
    (mulog/start-publisher! {:type :console, :pretty? true}))
  (-> config-fname config-fname->map create-system))

(c-repl/set-init mk-system)

(defn start-dev []
  (c-repl/start)
  :started)
(defn stop-dev []
  (c-repl/stop)
  :stopped)
(defn restart-dev []
  (c-repl/reset)
  :restarted)

(comment
  ; (resultset-seq)

  (keys c-repl/system)  ;; => '(:database :http-server)
  (:http-server c-repl/system)
  (restart-dev)
  (start-dev)
  (stop-dev)

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