(ns integrated-learning-system.handlers.commons.html
  (:require [ring.util.http-response :as responses]
            [ring.util.mime-type :refer [default-mime-types]]))


(defn resp-200
  ([body-doc] (-> body-doc
                  (responses/ok)
                  (responses/content-type "text/html; charset=UTF-8")))
  ([] (resp-200 nil)))

(defn resp-400
  ([body-doc] (-> body-doc
                  (responses/bad-request)
                  (responses/content-type (default-mime-types "html"))))
  ([] (resp-400 nil)))

(defn resp-404
  ([body-doc] (-> body-doc
                  (responses/not-found)
                  (responses/content-type (default-mime-types "html"))))
  ([] (resp-404 nil)))

(defn resp-500
  ([body-doc] (-> body-doc
                  (responses/internal-server-error)
                  (responses/content-type (default-mime-types "html"))))
  ([] (resp-500 nil)))

(defn resp-501
  ([body-doc] (-> body-doc
                  (responses/not-implemented)
                  (responses/content-type (default-mime-types "html"))))
  ([] (resp-501 nil)))
