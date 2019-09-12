(ns kestrel.kestrel
  (:require [result.result :as r]
            [result.service :refer :all])
  (:use [xml.file-consumer :only [read-file-or-files]]
        util.util)
  (:import org.hogel.kestrel.SimpleKestrelClient
           java.nio.file.Paths
           java.nio.file.Files))

(def conns (atom {}))

(defn queue-name [queue timeout]
  (str queue "/t=" timeout))

(defaction consume-action
  "alias|queue|number|wait(|regex)*
  Consume messages from a kestrel queue. The server matches the alias
  from the config file and consumes from the given queue. The number
  of messages and how long it should wait max until it has consumed
  all messages are given. For the regexes, at least one regex must
  match each message."
  [identifier _ _ alias queue number wait & regexes]
  (let [msgs (map (fn [_] (.get (get @conns alias)
                                queue (Integer/parseInt wait)))
                  (range (Integer/parseInt number)))
        pats (partial exists-patterns regexes)
        results (map #(if (nil? %) [nil nil] (pats %)) msgs)]
    (reify r/ResultProtocol
     (r/identifier [_] identifier)
     (r/expected [_] number)
     (r/result? [_] (and (= (count msgs) (Integer/parseInt number))
                         (exists-results results)))
     (r/actual [_] results)
     (r/failures [_] (exists-failed-results results)))))

(defaction bin-consume-action
  "alias|queue|number|wait
  Consume binary formatted messages from a kestrel queue. The server
  matches the alias from the config file and consumes from the given
  queue. The number of messages and how long it should wait max until
  it has consumed all messages are given. As this is binary data, no
  attempt at regex is attempted."
  [identifier _ _ alias queue number wait & _]
  (let [msgs (map (fn [_] (.getByte (get @conns alias)
                                    queue (Integer/parseInt wait)))
                  (range (Integer/parseInt number)))
        results (filter #(not (nil? %)) msgs)]
    (let [num (Integer/parseInt number)]
      (reify r/ResultProtocol
        (r/identifier [_] identifier)
        (r/expected [_] num)
        (r/result? [_] (= (count results) num))
        (r/actual [_] results)
        (r/failures [_] [(- num (count results))])))))

(defaction publish-action
  "alias|queue|path
  Publish messages to a kestrel queue. The server matches the alias
  from the config file and publish to the given queue. The path
  describes where messages may be published from, using one or more
  files and assuming XML input."
  [identifier _ _ alias queue msg-path & _]
  (doseq [[xml _] (read-file-or-files msg-path)]
    (.set (get @conns alias) queue 3600 xml))
  (reify r/ResultProtocol
    (r/identifier [_] identifier)
    (r/expected [_] "messages sent")
    (r/result? [_] true)
    (r/actual [_] "?")
    (r/failures [_] nil)))

(defaction bin-publish-action
  "alias|queue|path
  Publish binary messages to a kestrel queue. The server matches the
  alias from the config file and publishes to the given queue. Only
  one message can be published at a time."
  [identifier _ _ alias queue msg-path & _]
  (let [path (Paths/get "." (into-array String [msg-path]))
        message (Files/readAllBytes path)]
    (.set (get @conns alias) queue 3600 message))
  (reify r/ResultProtocol
    (r/identifier [_] identifier)
    (r/expected [_] "messages sent")
    (r/result? [_] true)
    (r/actual [_] "?")
    (r/failures [_] nil)))

(defaction peek-action
  "alias|queue|wait(|regex)
  Look at messages at the top of the queue defined by alias in the
  config file. Wait the maximum amount of time. At least one message
  must match."
  [identifier _ _ alias queue wait & regexes]
  (let [msg (.peek (get @conns alias)
                   queue (Integer/parseInt wait))
        pats (partial exists-patterns regexes)
        results (if (nil? msg) [nil nil] [(pats msg)])]
    (reify r/ResultProtocol
     (r/identifier [_] identifier)
     (r/expected [_] 1)
     (r/result? [_] (exists-results results))
     (r/actual [_] results)
     (r/failures [_] (exists-failed-results results)))))

(defaction delete-action
  "alias|queue
  Using the server provided by the alias in the config, delete the
  queue."
  [identifier _ _ alias queue & _]
  (.delete (get @conns alias) queue)
  (reify r/ResultProtocol
    (r/identifier [_] identifier)
    (r/expected [_] (str alias ":" queue ": deleted"))
    (r/result? [_] true)
    (r/actual [_] (str alias ":" queue ": deleted"))
    (r/failures [_] [])))

(defn kestrel-service [m]
  (reify Service
    (initialize [_]
      (let [c (into {}
                    (map (fn [[k v]]
                           (let [s (clojure.string/split
                                    (get v "servers")
                                    #":")
                                 host (first s)
                                 port (Integer/parseInt (second s))]
                             [k (SimpleKestrelClient. host port)])) m))]
        (swap! conns (fn [old] (merge old c)))))
    (service-name [_] "kestrel")
    (services [_] {"consume" consume-action
                   "publish" publish-action
                   "bin-consume" bin-consume-action
                   "bin-publish" bin-publish-action
                   "peek" peek-action
                   "delete" delete-action})
    ;; (perform-service [_ args]
    ;;   (apply kestrel-actions args))
    (close [_]
      (doseq [[_ connection] @conns]
        (.close connection)))))
