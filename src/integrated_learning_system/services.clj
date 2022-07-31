(ns integrated-learning-system.services
  "Life-cycle definitions for stateful services"
  (:require [integrant.core :as ig]
            [clojure.java.io :as io]
            [clojure.spec.alpha :as s]
            [com.brunobonacci.mulog :as mulog]
            [aero.core :as aero])
  (:import (java.net URL)
           (java.io FileNotFoundException)))

(s/def ::log-event #{::resource-config-file-not-found})

(defmethod aero/reader 'ig/ref
  ; Educate `aero` on how to read `#ig/ref` literal tag
  [_ _ value]
  (ig/ref value))

(defn config-fdir->map [config-fname]
  (if-some [^URL config-file (io/resource config-fname)] ;; find the `.edn` config in `resources` folder
    (aero/read-config config-file)
    (do
      (mulog/log ::resource-config-file-not-found :config-file-name config-fname)
      ;(throw (FileNotFoundException. config-fname))
      nil)))

(defn config->service-map [config-fdir]
  (some-> config-fdir config-fdir->map ig/prep ig/init))