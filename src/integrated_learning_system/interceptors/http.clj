(ns integrated-learning-system.interceptors.http
  (:require [reitit.coercion :as reitit-coercion]
            [reitit.impl :as reitit-impl]
            [reitit.spec :as reitit-spec]))


;region coercion

(defn safe-coerce-request-interceptor
  "`reitit.http.coercion/coerce-request-interceptor` but does not throw.
  Expects a :coercion of type `reitit.coercion/Coercion`
  and :parameters from route data, otherwise does not mount."
  []
  {:name    ::coerce-request
   :spec    ::reitit-spec/parameters
   :compile
   (fn [{:keys [coercion parameters]} opts]
     (cond
       ;; no coercion, skip
       (not coercion) nil
       ;; just coercion, don't mount
       (not parameters) {}
       ;; mount
       :else
       (if-let [coercers (reitit-coercion/request-coercers coercion parameters opts)]
         {:enter (fn [ctx]
                   (let [request (:request ctx)
                         [k v] (try
                                 [:parameters (reitit-coercion/coerce-request coercers request)]
                                 (catch Exception exn
                                   ; OBSERVATION:
                                   ; `v` is of the same shape as return value from `clojure.spec.alpha/explain-data`
                                   [:coercion-problems (get-in (Throwable->map exn)
                                                               [:data :problems])]))
                         request (reitit-impl/fast-assoc request k v)]
                     (assoc ctx :request request)))}
         {})))})

;endregion
