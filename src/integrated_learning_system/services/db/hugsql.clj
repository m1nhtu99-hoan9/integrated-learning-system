(ns integrated-learning-system.services.db.hugsql
  "Overrides handling of some hugsql command & result types."
  (:require [clojure.walk :refer [postwalk]]
            [hugsql.adapter]
            [hugsql.core :refer [hugsql-command-fn hugsql-result-fn]]
            [integrated-learning-system.db :as db]
            [java-time.api :as jt]))


(defn- traverse-inst->local-date-time
  "Exhaustively convert all java.util.Date values in map to java.time.LocalDateTimes.
   Reference: https://github.com/layerware/hugsql/issues/92#issuecomment-490421667"
  [map]
  (postwalk #(if-not (inst? %)
               %
               (jt/local-date-time %))
            map))

(defn query [adapter db-conn sqlvec options]
  (->> (hugsql.adapter/query adapter db-conn sqlvec options)
       traverse-inst->local-date-time))

(defn result-one [adapter result options]
  (->> (hugsql.adapter/result-one adapter result options)
       traverse-inst->local-date-time))

(defn result-many [adapter result options]
  (->> (hugsql.adapter/result-many adapter result options)
       traverse-inst->local-date-time))

; https://github.com/layerware/hugsql/blob/201d163a84f1493a2a73d5953b77d2e9d23f62e7/hugsql-core/src/hugsql/core.clj#L236

(defmethod hugsql-command-fn :<! [_] `query)
(defmethod hugsql-command-fn :returning-execute [_] `query)
(defmethod hugsql-command-fn :? [_] `query)
(defmethod hugsql-command-fn :query [_] `query)
(defmethod hugsql-command-fn :default [_] `query)

(defmethod hugsql-result-fn :1 [_] `result-one)
(defmethod hugsql-result-fn :one [_] `result-one)
(defmethod hugsql-result-fn :* [_] `result-many)
(defmethod hugsql-result-fn :many [_] `result-many)
