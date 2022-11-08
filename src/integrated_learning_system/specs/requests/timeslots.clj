(ns integrated-learning-system.specs.requests.timeslots
  (:require [clojure.spec.alpha :as s]
            [integrated-learning-system.specs.commons.validation-messages :as vms]
            [integrated-learning-system.utils.datetime :as dt])
  (:import [java.time LocalTime]))

(defonce validation-messages
  {::duration-mins-valid (fn [req]
                           (str "durationMins value is expected to be a positive integer, but found: "
                                (:duration-mins req))),
   ::start-at-valid      (fn [req]
                           (vms/prop-time-invalid-value "startAt"
                                                        (:start-at req)
                                                        (:time/without-seconds dt/time-patterns)))})

(s/def ::duration-mins-valid #(pos-int? %))
(s/def ::duration-mins (s/and ::duration-mins-valid))

(s/def ::start-at-valid #(instance? LocalTime %))
(s/def ::start-at (s/and ::start-at-valid))

;-- timeslot-add-request

(s/def ::timeslot-add-request (s/keys :req-un [::start-at ::duration-mins]))

(defn body-params->timeslot-add-request [{:as body-params, :keys [start-at]}]
  (-> body-params
      (select-keys [:duration-mins])
      (assoc :start-at (dt/string->local-time start-at :time/without-seconds))))