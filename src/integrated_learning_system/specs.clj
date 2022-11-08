(ns integrated-learning-system.specs
  "Shared `spec` specifications"
  (:require [clojure.spec.alpha :as s]
            [com.brunobonacci.mulog :as mulog]))

(defonce semver-regex
  #"^(0|[1-9]\d*)\.(0|[1-9]\d*)\.(0|[1-9]\d*)(?:-((?:0|[1-9]\d*|\d*[a-zA-Z-][0-9a-zA-Z-]*)(?:\.(?:0|[1-9]\d*|\d*[a-zA-Z-][0-9a-zA-Z-]*))*))?(?:\+([0-9a-zA-Z-]+(?:\.[0-9a-zA-Z-]+)*))?$")

(defonce email-regex
  ; reference: https://owasp.org/www-community/OWASP_Validation_Regex_Repository
  #"^[a-zA-Z0-9_+&*-]+(?:\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\.)+[a-zA-Z]{2,15}$")

(defonce phone-num-regex
  ; reference: https://stackoverflow.com/a/16702965
  #"^\s*(?:\+?(\d{1,3}))?[-. (]*(\d{3})[-. )]*(\d{3})[-. ]*(\d{4})(?: *x(\d+))?\s*$")

(s/def ::semver
  (s/and string? #(re-matches semver-regex %)))

(s/def ::email
  (s/and string? #(re-matches email-regex %)))

(s/def ::phone-num
  (s/and string? #(re-matches phone-num-regex %)))

(s/def ::port (s/or :raw string?
                    :parsed pos-int?))

(defn- lookup-validation-message-map [defined-message-map keyword]
  (let [msg (defined-message-map keyword)]
    (when (nil? msg)
      (mulog/log ::validation-message-undefined :keyword keyword))
    msg))

(defn- spec-problem->validation-problem [defined-messages spec-problem orig-value]
  (let [prop-name (-> spec-problem :path last)
        message-resolver (->> spec-problem :via last (lookup-validation-message-map defined-messages))
        ; `message-resolver` is either a unary-fn or a string constant
        message (if (fn? message-resolver)
                  (message-resolver orig-value)
                  message-resolver)]
    [(or prop-name :others) message]))

(defn- spec-explanation->validation-result*
  [defined-messages spec-explanation orig-value]
  (when-some [probs (::s/problems spec-explanation)]
    ; event logging
    (mulog/log ::entered--spec-explanation->validation-result
               :defined-validation-messages defined-messages
               :spec-explanation spec-explanation)
    ; processing
    (letfn [(reduce-to-valid-probs [validation-probs spec-prob]
              (comment (str "With `spec-prob` resolves to prop 'p' and validation message 'm': "
                            "If 'm' is null, do nothing. Otherwise, aggregate it to `validation-probs` under 'p' key."))
              (let [[prop-kw msg] (spec-problem->validation-problem defined-messages spec-prob orig-value)
                    validation-msgs (validation-probs prop-kw)]
                (if (nil? msg)
                  validation-probs
                  (assoc! validation-probs
                          prop-kw
                          (if (nil? validation-msgs)
                            [msg]
                            (conj validation-msgs msg))))))]
      (persistent!
        (reduce reduce-to-valid-probs (transient {}) probs)))))


(defn- spec-explanation->validation-result
  ([defined-messages spec-explanation orig-value]
   (try
     (spec-explanation->validation-result* defined-messages spec-explanation orig-value)
     (catch Exception exn
       (mulog/log ::failed-spec-explanation->validation-result
                  :exn exn)
       nil)))
  ([defined-messages spec-explanation]
   spec-explanation->validation-result defined-messages spec-explanation (::s/value spec-explanation)))


(defn spec-validate
  "Validate candidate using clojure.spec.alpha/explain-data, then transform the result to user-friendly result map.
  If the optional arg value orig-candidate is provided, it's regarded as pre-transformed/pre-coerced version of candidate."
  [spec validation-messages candidate & {:keys [orig-candidate]}]

  ; CAVEAT: Because `clojure.spec` is still in alpha, and is not designed for use case of data validation,
  ;  `clojure.spec.alpha/explain-data` does not coerce input while performing validation.
  ;  Reference: https://stackoverflow.com/a/49056441

  (as-> candidate $
        (s/explain-data spec $)
        (spec-explanation->validation-result validation-messages $ (or orig-candidate candidate))))

