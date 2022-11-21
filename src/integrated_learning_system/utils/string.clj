(ns integrated-learning-system.utils.string
  (:import [java.nio.charset StandardCharsets Charset]))

(defn utf8-string [string]
  (let [charset StandardCharsets/UTF_8]
    (String. (.getBytes string charset)
             ^Charset charset)))
