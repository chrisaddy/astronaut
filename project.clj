(defproject astronaut "0.1.0"
  :description "Spaced-Repition for Good Learnin'"
  :url "http://example.com/FIXME"
  :license {:name "MIT"
            :url "https://opensource.org/licenses/MIT"}
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [cli-matic "0.3.11"]
                 [org.clojure/java.jdbc "0.7.11"]
                 [clj-time "0.15.2"]
                 [org.xerial/sqlite-jdbc "3.7.2"]
                 [org.clojure/math.numeric-tower "0.0.4"]]
  :main ^:skip-aot astronaut.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}
             :dev {:plugins [[lein-binplus "0.6.6"]]}}
  :bin {:name "astronaut"
        :bin-path "~/bin"})
