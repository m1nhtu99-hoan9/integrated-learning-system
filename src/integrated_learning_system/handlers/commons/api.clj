(ns integrated-learning-system.handlers.commons.api
  (:require [ring.util.http-response :as responses]
            [ring.util.http-status :as status]
            [org.apache.commons.lang3.StringUtils :refer [*blank?]]))

(defn resp-200
  ([payload] (responses/ok payload))
  ([] (resp-200 nil)))

(defn resp-201
  ([uri payload]
   (let [actual-uri (if (*blank? uri)
                      nil
                      uri)]
     {:status 201,
      :headers {"Location" actual-uri},
      :body (assoc payload :uri actual-uri)}))
  ([uri] (resp-201 uri nil))
  ([] (resp-201 nil)))

(defn resp-401
  ([title errors-payload]
   {:status 401
    :body   {:title  title
             :errors errors-payload}})

  ([errors-payload] (resp-401 (status/get-description 401) errors-payload))
  ([] (resp-401 nil)))

(defn resp-422
  ([title errors-payload]
   {:status 422
    :body   {:title  title
             :errors errors-payload}})

  ([errors-payload] (resp-422 (status/get-description 422) errors-payload))
  ([] (resp-422 nil)))

(defn resp-500
  ([title errors-payload]
   {:status 500
    :body   {:title  title
             :errors errors-payload}})

  ([errors-payload] (resp-500 (status/get-description 500) errors-payload))
  ([] (resp-500 nil)))


(defn resp-501
  ([title details]
   {:status 501
    :body   {:title   title
             :details details}})
  ([title] (resp-501 title nil))
  ([] (resp-501 (status/get-name 501)
                (status/get-description 501))))

