(ns integrated-learning-system.spec
  "Shared `spec` specifications"
  (:require [clojure.spec.alpha :as s]))

(defonce ^:private semver-regex
  #"^(0|[1-9]\d*)\.(0|[1-9]\d*)\.(0|[1-9]\d*)(?:-((?:0|[1-9]\d*|\d*[a-zA-Z-][0-9a-zA-Z-]*)(?:\.(?:0|[1-9]\d*|\d*[a-zA-Z-][0-9a-zA-Z-]*))*))?(?:\+([0-9a-zA-Z-]+(?:\.[0-9a-zA-Z-]+)*))?$")

(s/def ::semver
  (s/and string? #(re-matches semver-regex %)))
