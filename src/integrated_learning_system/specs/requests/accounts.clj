(ns integrated-learning-system.specs.requests.accounts
  (:require [clojure.spec.alpha :as s]
            [integrated-learning-system.specs :as specs]
            [integrated-learning-system.specs.commons.validation-messages :as vms]
            [integrated-learning-system.utils.datetime :as dt]
            [org.apache.commons.lang3.StringUtils :refer [*equals-ignore-case *not-blank?]])
  (:import (java.time LocalDate)))

(defn- string->account-role [string]
  (condp *equals-ignore-case string
    nil :admin
    "admin" :admin
    "teacher" :teacher
    "student" :student
    :unknown))

(defonce validation-messages
         {::username-nonempty       (vms/prop-string-nonempty "username")
          ::username-length         (vms/prop-string-length-between "username" 5 100)
          ::username-chars          "username can only contain alphabetic, numeric, '_', '.' or '-' characters."
          ::password-nonempty       (vms/prop-string-nonempty "password")
          ::role-defined            (fn [req]
                                      (vms/prop-enum-invalid-value "role"
                                                                   (vector "admin" "teacher" "student")
                                                                   (:role req)))
          ::first-name-nonempty     (vms/prop-string-nonempty "firstName")
          ::first-name-length       (vms/prop-string-maxlength "firstName" 50)
          ::last-name-nonempty      (vms/prop-string-nonempty "lastName")
          ::last-name-length        (vms/prop-string-maxlength "lastName" 50)
          ::date-of-birth-valid     (fn [req]
                                      (vms/prop-date-invalid-value "dateOfBirth" (:date-of-birth req)))
          ::personal-email-nonempty (vms/prop-string-nonempty "personalEmail")
          ::personal-email-length   (vms/prop-string-maxlength "personalEmail" 50)
          ::personal-email-valid    (fn [req]
                                      (vms/prop-string-email-invalid "personalEmail" (:personal-email req)))
          ::phone-number-nonempty   (vms/prop-string-nonempty "phoneNumber")
          ::phone-number-valid      (fn [req]
                                      (vms/prop-string-phonenum-invalid "phoneNumber" (:phone-number req)))})


(s/def ::username-nonempty #(*not-blank? %))
(s/def ::username-length #(<= 5 (.length %) 100))
(s/def ::username-chars #(re-matches #"^[a-zA-Z0-9_.\-]+" %))
(s/def ::username (s/and ::username-nonempty ::username-length ::username-chars))

(s/def ::password-nonempty #(*not-blank? %))
(s/def ::password (s/and ::password-nonempty))

(s/def ::role-defined #{:admin :teacher :role})
(s/def ::role (s/and ::role-defined))

(s/def ::first-name-nonempty #(*not-blank? %))
(s/def ::first-name-length #(<= (.length %) 50))
(s/def ::first-name (s/and ::first-name-nonempty ::first-name-length))

(s/def ::last-name-nonempty #(*not-blank? %))
(s/def ::last-name-length #(<= (.length %) 50))
(s/def ::last-name (s/and ::last-name-nonempty ::last-name-length))

(s/def ::date-of-birth-valid #(instance? LocalDate %))
(s/def ::date-of-birth (s/and ::date-of-birth-valid))

(s/def ::personal-email-nonempty #(*not-blank? %))
(s/def ::personal-email-length #(<= (.length %) 64))
(s/def ::personal-email-valid #(re-matches specs/email-regex %))
(s/def ::personal-email (s/and ::personal-email-nonempty ::personal-email-valid ::personal-email-length))

(s/def ::phone-number-nonempty #(*not-blank? %))
(s/def ::phone-number-valid #(re-matches specs/phone-num-regex %))
(s/def ::phone-number (s/and ::phone-number-nonempty ::phone-number-valid))

;-- ::account-add-request

(defn body-params->account-add-request [{:as body-params, :keys [role date-of-birth]}]
  (assoc body-params :role (string->account-role role)
                     :date-of-birth (some-> date-of-birth dt/string->local-date)))

(s/def ::account-add-request (s/keys :req-un [::role]
                                     :opt-un [::date-of-birth]))

(s/def ::-account-add-payload (s/keys :req-un [::username ::password]
                                      :opt-un [::personal-email ::phone-number]))
(s/def ::account-add-admin-payload
  (s/merge ::-account-add-payload
           (s/keys :opt-un [::first-name ::last-name])))
(s/def ::account-add-teacher-payload
  (s/merge ::-account-add-payload
           (s/keys :req-un [::first-name ::last-name])))
(s/def ::account-add-student-payload
  (s/merge ::-account-add-payload
           (s/keys :req-un [::first-name ::last-name])))

(defn select-account-add-payload-spec [role]
  (case role
    :admin ::account-add-admin-payload
    :teacher ::account-add-teacher-payload
    :student ::account-add-student-payload))
