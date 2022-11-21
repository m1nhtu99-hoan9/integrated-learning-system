(ns integrated-learning-system.utils.json
  (:require [camel-snake-kebab.core :as csk]
            [jsonista.core :as j]))

(defonce jsonista-mapper
  (j/object-mapper {:encode-key-fn csk/->camelCaseString
                    :decode-key-fn csk/->kebab-case-keyword}))

(defn read-json [object]
  (j/read-value object jsonista-mapper))

(defn ->json-string [object]
  (j/write-value-as-string object jsonista-mapper))