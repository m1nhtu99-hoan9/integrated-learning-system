(require '[clojure.string :as str])

(defn- compare-namespaces [ns1 ns2]
  (let [[my-ns1 my-ns2] (map
                          #(-> % (str/split #"integrated-learning-system\.") second)
                          [ns1 ns2])]
    (case [(nil? my-ns1) (nil? my-ns2)]
      [true false] -1                                       ; place ns1 above
      [false true] 1                                        ; place ns2 above
      [true true] (compare ns1 ns2)
      (compare my-ns1 my-ns2))))

(defproject integrated-learning-system "0.1.0-SNAPSHOT"
  :description "Integrated Learning System supports attendance management & schoolwork management."
  :url "http://example.com/UNDEFINED"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url  "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [aero "1.1.6"]
                 [integrant "0.8.0"]
                 [metosin/reitit "0.5.18"]
                 [clj-http "3.12.3"]
                 [io.pedestal/pedestal.service "0.5.10"]
                 [io.pedestal/pedestal.jetty "0.5.10"]
                 [metosin/reitit-pedestal "0.5.18"]
                 [com.brunobonacci/mulog "0.9.0"]
                 [org.clojure/tools.analyzer "1.1.0"]]
  :repl-options {:init-ns integrated-learning-system.server}
  :profiles {:uberjar {:aot :all}
             :dev     {:resource-paths ["dev/resources"]
                       :source-paths   ["dev/src"]
                       :dependencies   [[integrant/repl "0.3.1"]]}}
  :plugins [[lein-nsort "0.1.15"]]
  :uberjar-name "integrated-learning-system.jar"
  :nsort {:source-paths ["src" "dev/src"]
          :require      {:comp compare-namespaces}})
