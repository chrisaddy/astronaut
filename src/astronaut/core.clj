(ns astronaut.core
  (:require [clojure.tools.cli :refer [parse-opts]])
  (:gen-class))

(def cli-options
  [[]
   ["-h" "--help"]]
  )

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (:bin {:bin-path "~/bin" :bootclasspath true})
  (parse-opts args cli-options)
  (println (str cli-options))
  (println "Hello, World!"))
