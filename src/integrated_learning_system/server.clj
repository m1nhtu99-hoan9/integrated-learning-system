(ns integrated-learning-system.server
  (:require
    [aero.core :as aero]
    [clojure.java.io :as io]
    [clojure.spec.alpha :as s]
    [com.brunobonacci.mulog :as mulog]
    [com.brunobonacci.mulog.core :refer [publishers]]
    [integrant.core :as ig]
    integrated-learning-system.services.db
    integrated-learning-system.services.server)
  (:import
   [clojure.lang MapEntry]
   [com.brunobonacci.mulog.publisher ConsolePublisher]
   [java.io FileNotFoundException]))

(s/def ::state #{:started :resumed :stopped :restarted :failed})
(s/def ::log-event #{::resource-config-file-not-found ::config-resolved})

(defmethod aero/reader 'ig/ref [_ _ value]
  ; Educate `aero` on how to read `#ig/ref` literal tag
  (ig/ref value))

(defn config-fname->map
  "Read from and then resolve config-fname to get config map"
  [config-fname]
  (if-some [config (some-> config-fname (io/resource) (aero/read-config))] ; find the `.edn` config under `resources` folder
    (do
      (mulog/log ::config-resolved :content config)
      config)
    (do
      (mulog/log ::resource-config-file-not-found :file-name config-fname)
      (throw (FileNotFoundException. config-fname)))))

(defn start-console-log-publisher!
  "Register a new mulog console log publisher only when there are none yet"
  []
  (when (not-any? (fn [publisher]
                    (->> ^MapEntry publisher
                         (.getValue)
                         (:publisher)
                         (instance? ConsolePublisher)))
                  @publishers)
    (mulog/start-publisher! {:type :console, :pretty? true})))


(defn -main [config-fname]
  (start-console-log-publisher!)

  (if-some [config (config-fname->map config-fname)]
    ; resolve & kick-start DI
    (-> config
        (ig/prep)
        (ig/init [:server/app :server/http :db/postgres]))))

(comment
  (-main "config.edn"))
