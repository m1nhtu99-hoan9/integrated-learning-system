(ns integrated-learning-system.db.sql.commons)

(defn path-to-sql [stem]
  (str "integrated_learning_system/db/sql/" stem ".sql"))
