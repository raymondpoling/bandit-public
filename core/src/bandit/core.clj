(ns bandit.core
  (:require [bandit.deline :as deline]
            [clj-http.client :as client]
            [result.service :refer :all]
            [result.result :as r])
  (:use
   ;;    [rest.rest :only [rest-service]]
   [comment.comment :only [comment-service]]
   ;; [mongo.mongo-service :only [mongo-service]]
   ;; [jms.jms :only [jms-service]]
   [result.result :only [choose-printer result?]]
   ;; [shell.shell :only [shell-service]]
   ;; [websocket.websocket :only [websocket-service]]
   ;; [kestrel.kestrel :only [kestrel-service]]
   )
  (:import java.io.StringWriter

           java.io.PrintWriter)
  (:gen-class))

(def service-list (atom {}))

(defn init-service [service-name config]
  (require (symbol (str service-name "." service-name)))
  (let [service ((resolve (symbol (str service-name "."
                                       service-name "/"
                                       service-name "-service")))
                 config)]
    service))

(defn ensure-service [service-name config init-function]
  (let [service-exists (get @service-list service-name)]
    (cond
     (or (and (nil? service-exists)
              (get config service-name))
         (= "comment" service-name))
     (let [service  (get (swap! service-list
                                (fn [m] (merge m {service-name
                                                 (init-service
                                                  service-name
                                                  (get config service-name))})))
                         service-name)]
       (init-function service)
       service)
     (not (get config service-name)) (failed-result service-name)
     :else service-exists)))

(defn -main
  "Get a file name, and use it to load
files for testing with http."
  [& args]
  (let [number-of-arguments (count args)]
    (if (or (> number-of-arguments 2) (<= number-of-arguments 3))
      (let [config (eval (read-string (slurp (first args))))
            csv (case (second args)
                  "-" (deline/get-split-file-lines
                       (line-seq (java.io.BufferedReader. *in*)))
                  "help" (let [keySet (keys config)]
                           (doseq [k keySet]
                             (let [service
                                   (ensure-service k config (fn [x] x))]
                               (documentation service)))
                           (System/exit 0))
                  (reduce
                   concat
                   (map #(cons [(str "File: " %1) "comment" "comment" %1]
                               (deline/get-split-file-lines
                                (line-seq (clojure.java.io/reader %1))))
                        (clojure.string/split (second args) #","))))
            printer (if (= (count args) 3)
                      (choose-printer (nth args 2))
                      (choose-printer nil))
            pass (atom true)]
        (println (printer ["Running Tasks"]))
        (doseq [line csv]
          (try
            (println)
            (println (printer line))
            (let [result (apply perform-service
                                (ensure-service (second line)
                                                config initialize) [line])]
              (if-not (r/result? result)
                (swap! pass (constantly false)))
              (println (printer result)))
            (catch Exception e
(let [se  (StringWriter.)
      pw (PrintWriter. se)]
  (.printStackTrace e pw)
  (println
   (printer
    (reify r/ResultProtocol
      (r/identifier [_] "This bandit")
      (r/expected [_] "all lines run")
      (r/result? [_] false)
      (r/actual [_] "an exception was thrown")
      (r/failures [_] (into '()
                            (clojure.string/split
                             (.toString se) #"\n")))
      )))))))
        (doseq [[_ service] @service-list] (close service))
        (shutdown-agents)
        (if-not @pass
          (System/exit 254)))



      (println "Arguments: [config.cfg] [test.csv] [printer?]"))))
