(ns integrated-learning-system.server
  (:require
    [aero.core :as aero]
    [clojure.java.io :as io]
    [clojure.spec.alpha :as s]
    [com.brunobonacci.mulog :as mulog]
    [com.brunobonacci.mulog.core :refer [publishers]]
    [com.stuartsierra.component :as component]
    [integrated-learning-system.components.database :refer [database-component]]
    [integrated-learning-system.components.http-server :refer [http-server-component]]
    [integrated-learning-system.migration :refer [init-db!]]
    [integrated-learning-system.specs.config :as s-config])
  (:import
    (com.brunobonacci.mulog.publisher ConsolePublisher)
    (java.io FileNotFoundException)))

(s/def ::state #{:started :resumed :stopped :restarted :failed})
(s/def ::log-event #{::resource-config-file-not-found ::config-resolved})

(defn config-fname->map [config-fname]
  "Read from and then resolve config-fname to get config map"
  (if-some [config (some-> config-fname (io/resource) (aero/read-config))]          ; find the `.edn` config under `resources` folder
    (do
      (mulog/log ::config-resolved :content config)
      config)
    (do
      (mulog/log ::resource-config-file-not-found :file-name config-fname)
      (throw (FileNotFoundException. config-fname)))))

(defn create-system [config-map]
  "Create Component system"
  (component/system-map
    ; :config config-map
    :database (database-component (get-in config-map [:db :postgres]))
    :http-server (component/using (http-server-component (config-map :server))
                                  {:db-conn :database})))

(defn -main [config-fname]
  ; register a new mulog console log publisher only when there are none yet
  (when (not-any? #(instance? ConsolePublisher %) @publishers)
    (mulog/start-publisher! {:type :console, :pretty? true}))

  (if-some [config (config-fname->map config-fname)]
    (do
      ; validate config
      (when (s/invalid? (s/conform ::s-config/config-map config))
        (throw (ex-info "Invalid config"
                        (s/explain-data ::s-config/config-map config))))
      ; run init migration script (WIP)
      (init-db! (config :db))
      ; resolve & kick-start DI
      (component/start (create-system config)))))


(comment
  (-main "config.edn"))