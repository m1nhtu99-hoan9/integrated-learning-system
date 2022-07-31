(ns integrated-learning-system.routing.students)

(defn- handle-get-all [request]
  {:status 200, :body "Get all students"})

(defn create-student-routes []
  {:get {:responses {200 {:body string?}}
         :handler handle-get-all}
   :swagger {:tags ["students"]}})
