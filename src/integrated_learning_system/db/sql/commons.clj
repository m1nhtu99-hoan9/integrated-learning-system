(ns integrated-learning-system.db.sql.commons)

(defn path-to-sql
  "Returns the classpath to '{stem}.sql' file."
  [stem]
  (str "integrated_learning_system/db/sql/" stem ".sql"))
