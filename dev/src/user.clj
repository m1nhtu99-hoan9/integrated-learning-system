(ns user
  (:require
    [clojure.pprint :refer [pprint]]
    [clojure.spec.alpha :as s]
    [integrant.repl :refer :all]
    [integrant.core :as ig]
    [integrant.repl.state :as state]
    [integrated-learning-system.server :as server]
    [integrated-learning-system.services :refer [config->service-map config-fdir->map]]
    [com.brunobonacci.mulog :as mulog]))

(defonce ^:private config-fname "config-dev.edn")

(mulog/start-publisher! {:type :console})
(set-prep! #(config-fdir->map config-fname))

(defn start-dev []
  {:post [(s/valid? ::server/state %)]}

  (try
    (go)
    (catch Exception _
      :failed))
  :started)

(defn halt-dev []
  {:post [(s/valid? ::server/state %)]}

  (halt)
  :stopped)

(defn restart-dev []
  {:post [(s/valid? ::server/state %)]}
  (halt-dev)
  (start-dev)

  :restarted)

(comment
  (def server-impl (:server/app state/system))
  server-impl
  (pprint server-impl)
  (restart-dev)
  (start-dev)
  (stop-dev))

;; ring experiments
(comment (def app (-> state/system :server/app)))
(comment (set-prep! #(-> "resources/config.edn" slurp ig/read-string)))
(comment
  (go)                                                      ;; => :initiated
  (halt)                                                    ;; => :halted
  ;; REPL command added and bound to <ALT> + <SHIFT> + ,
  (reset)                                                   ;; => :resumed

  ;; Without the muuntaja 'format-middleware', opening this URI through web browser would lead to
  ;; HTTP ERROR 500 java.lang.IllegalArgumentException:
  ;; No implementation of method: :write-body-to-stream of protocol: #'ring.core.protocols/StreamableResponseBody found
  ;; for class: clojure.lang.PersistentArrayMap
  (app {:request-method :get
        :uri            "/swagger.json"})

  ;; Misc stuffs
  (ancestors (type '(1 2 3)))
  (ancestors (type [1 2 3])))

