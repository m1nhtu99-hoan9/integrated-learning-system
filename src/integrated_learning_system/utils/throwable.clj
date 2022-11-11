(ns integrated-learning-system.utils.throwable)


(defn exn->map
  ([exn trace-handler]
   (let [map (Throwable->map exn)]
     (-> map
         (update :trace #(trace-handler %)))))

  ([exn]
   ; dissoc :trace
   (exn->map exn (constantly nil))))
