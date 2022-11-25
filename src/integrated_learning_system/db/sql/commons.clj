(ns integrated-learning-system.db.sql.commons
  (:require [integrated-learning-system.utils.json :refer [->json-string]])
  (:import [org.postgresql.util PGobject]))


(defn path-to-sql
  "Returns the classpath to '{stem}.sql' file."
  [stem]
  (str "integrated_learning_system/db/sql/" stem ".sql"))

(defn ->pgsql-jsonb [obj]
  (doto (PGobject.)
    (.setType "jsonb")
    (.setValue (->json-string obj))))

(defn ->pgsql-json [obj]
  (doto (PGobject.)
    (.setType "json")
    (.setValue (->json-string obj))))
