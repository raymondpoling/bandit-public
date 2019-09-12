(ns shell.shell
  (:require [result.result :as r]
            [result.service :refer :all]
            [me.raynes.conch.low-level :as sh])
  (:use [clojure.java.shell :only [sh]]
        [clojure.string :only [split]]))

(defaction shell-action
  "command(|args)*
  Runs the given shell command, applying all of the args as though they
  were space seperated and quoted. For example:
  |ls|-lh|/home/admin of this pc
  would translate to
  ls -lh \"/home/admin of this pc\""
  [identifier _ action & args]

  (let [result (apply sh/proc args)
        output (:out result)
        _ (future (let [b (byte-array (* 1024 1024))]
                    (try
                      (loop [l (.read output b)]
                        (if (> l 0)
                          (recur (.read output b))))
                      (catch Exception e (.printStackTrace e)))))
        failure (with-open [r (clojure.java.io/reader (:err result))]
                  (doall (line-seq r)))]
    (reify r/ResultProtocol
      (r/identifier [_] identifier)
      (r/expected [_] "status 0")
      (r/result? [_]  (= 0 (sh/exit-code result)))
      (r/actual [_] (str "status " (sh/exit-code result)))
      (r/failures [_] failure))))

(defn shell-service [m]
  (reify Service
    (initialize [_])
    (service-name [_] "shell")
    (services [_] {"run" shell-action})
    ;; (perform-service [_ args]
    ;;   (apply shell-action args))
    (close [_] nil)))
