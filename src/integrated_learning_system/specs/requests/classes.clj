(ns integrated-learning-system.specs.requests.classes
  (:require [clojure.spec.alpha :as s]
            [integrated-learning-system.specs.requests.courses :as s-courses]
            [integrated-learning-system.specs.commons.validation-messages :as vms]
            [integrated-learning-system.utils.datetime :as dt]
            [org.apache.commons.lang3.StringUtils :refer [*not-blank?]]
            [java-time.api :as jt])
  (:import [java.time LocalDate DayOfWeek]))


(defonce validation-messages
  (merge (select-keys s-courses/validation-messages [::s-courses/course-code-chars
                                                     ::s-courses/course-code-length
                                                     ::s-courses/course-code-nonempty])
         {::class-name-nonempty (vms/prop-string-nonempty "className")
          ::class-name-chars    "className can only contain alphabetic, numeric, '_', '.' or '-' characters."
          ::class-name-length   (vms/prop-string-length-between "className" 3 15)
          ::start-date "Ill-formed date value."
          ::end-date "Ill-formed date value."
          ::date-of-week-num "Invalid weekly schedule planning."
          ::timeslot-number "Invalid weekly schedule planning."}))

(s/def ::course-code ::s-courses/course-code)

(s/def ::class-name-nonempty #(*not-blank? %))
(s/def ::class-name-length #(<= 3 (.length %) 15))
(s/def ::class-name-chars #(re-matches #"^[a-zA-Z0-9_.\-]+" %))
(s/def ::class-name (s/and ::class-name-nonempty ::class-name-length ::class-name-chars))

;-- ::class-add-request

(s/def ::class-add-request (s/keys :req-un [::course-code ::class-name]))
