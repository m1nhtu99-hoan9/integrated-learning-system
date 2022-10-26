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
                 [metosin/spec-tools "0.10.5"]
                 [io.pedestal/pedestal.service "0.5.10"]
                 [io.pedestal/pedestal.jetty "0.5.10"]
                 [metosin/reitit-pedestal "0.5.18"]

                 [com.github.seancorfield/next.jdbc "1.2.796"]
                 [com.layerware/hugsql-core "0.5.3"]
                 [com.layerware/hugsql-adapter-next-jdbc "0.5.3"]
                 [org.postgresql/postgresql "42.3.4"]

                 [clojure-interop/apache-commons-lang "1.0.0"]
                 [com.brunobonacci/mulog "0.9.0"]
                 [camel-snake-kebab "0.4.3"]
                 [crypto-password "0.3.0"]
                 ;; transient dependencies required by `lein-nsort`
                 [org.clojure/tools.namespace "1.3.0"]
                 [com.rpl/specter "1.1.3"]
                 [rewrite-clj "1.1.45"]
                 ;; other transient dependencies
                 [org.clojure/tools.analyzer "1.1.0"]
                 [org.slf4j/slf4j-api "1.7.36"]]
  :repl-options {:init-ns integrated-learning-system.server}
  :profiles {:uberjar {:aot :all}
             :dev     {:resource-paths ["dev/resources"]
                       :source-paths   ["dev/src"]
                       :dependencies   [[integrant/repl "0.3.1"]
                                        ;; transient dependencies
                                        [clojure-complete "0.2.5"]
                                        [nrepl "0.8.3"]]}}
  ;; turn on the global setting for using `spec` as validation
  :injections [(require '[clojure.spec.alpha :as s])
               (s/check-asserts true)]
  :plugins [[lein-nsort "0.1.15"]]
  :uberjar-name "integrated-learning-system.jar"
  :nsort {:source-paths ["src" "dev/src"]
          :require      {:sort-fn first, :comp compare-namespaces}})
