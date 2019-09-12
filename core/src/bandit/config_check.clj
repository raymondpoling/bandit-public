(ns bandit.config-check
  (:use clojure.pprint)
  (:gen-class))

(defn -main
  "Just parses and pretty prints back the config files"
  [& args]
  (if (= 1 (count args))
    (pprint (eval (read-string (slurp (first args)))))
    (println "This takes one argument, a single config file to process
and print out")))
