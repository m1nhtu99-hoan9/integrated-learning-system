(ns integrated-learning-system.utils.config)

(defn ->port-number [port-env]
  (if (string? port-env)
    (Integer/parseInt port-env)
    port-env))
