(ns integrated-learning-system.specs.requests.courses
  (:require [clojure.spec.alpha :as s]
            [integrated-learning-system.specs.commons.validation-messages :as vms]
            [org.apache.commons.lang3.StringUtils :refer [*not-blank?]]))


(defonce validation-messages
  {::course-code-nonempty (vms/prop-string-nonempty "courseCode")
   ::course-code-length   (vms/prop-string-length-between "courseCode" 3 12)
   ::course-code-chars    "courseCode can only contain alphabetic, numeric, '_', '.' or '-' characters."
   ::course-name-nonempty (vms/prop-string-nonempty "courseName")
   ::course-name-length   (vms/prop-string-maxlength "courseName" 100)})

(s/def ::course-code-nonempty #(*not-blank? %))
(s/def ::course-code-length #(<= 3 (.length %) 12))
(s/def ::course-code-chars #(re-matches #"^[a-zA-Z0-9_.\-]+" %))
(s/def ::course-code (s/and ::course-code-nonempty ::course-code-length ::course-code-chars))

(s/def ::course-name-nonempty #(*not-blank? %))
(s/def ::course-name-length #(<= (.length %) 100))
(s/def ::course-name (s/and ::course-name-nonempty ::course-name-length))

(s/def ::description #(instance? String %))

;-- ::course-add-request

(s/def ::course-add-request (s/keys :req-un [::course-code ::course-name]
                                    :opt-un [::description]))
