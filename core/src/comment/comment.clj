(ns comment.comment
    (:require [result.result :as r]
            [result.service :refer :all])
  (:use [result.service]))

(defn comment-action [& args]
  (let [[identifier module action & _] args]
    (reify r/ResultProtocol
      (r/identifier [_] identifier)
      (r/expected [_] "Pass")
      (r/result? [_] true)
      (r/actual [_] "Pass")
      (r/failures [_] []))))

(defn comment-service [_]
  (reify Service
    (initialize [_])
    (service-name [this] "comment")
    (services [this] {"comment" comment-action})
    ;; (perform-service [this args]
    ;;   (comment-action args))
    (close [_] nil)))
