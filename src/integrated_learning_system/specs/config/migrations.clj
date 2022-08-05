(ns integrated-learning-system.specs.config.migrations
  (:require [clojure.spec.alpha :as s]
            [clojure.string :as string]))

(s/def ::migration-dir string?)
(s/def ::init-script (s/and string? #(string/ends-with? % ".sql")))
(s/def ::init-in-transaction? boolean?)

(s/def ::config-map (s/keys :req-un [::migration-dir ::init-script]
                            :opt-un [::init-in-transaction?]))
