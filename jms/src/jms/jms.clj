(ns jms.jms
  (:require [result.result :as r])
  (:use [xml.file-consumer :only [read-file-or-files]]
        util.util
        result.service)
  (:import java.util.Properties
           javax.naming.InitialContext
           javax.naming.Context
           javax.jms.Session))

(def connections (atom {}))

(defprotocol JMSAction
  (action [this rest])
  (close-jms [this]))

(defn publish-file-messages [producer filename]
  (binding [count 0]
    (doseq [[xml properties] (read-file-or-files filename)]
      (.send producer)
      (set! count (+ 1 count)))
    count))

(defn make-context [m]
  (let [seed (merge {Context/SECURITY_PRINCIPAL ""
                     Context/SECURITY_CREDENTIALS ""} m)
        p (Properties.)]
    (doseq [[k v] seed]
      (.put p (name k) v))
    (InitialContext. p)))

(defn pub-action
  [session producer args]
  (let [[identifier file & _] args]
          (doseq [[xml properties] (read-file-or-files file)]
            (let [message (.createTextMessage session xml)]
              (doseq [[k v] properties]
               (.setStringProperty message k v))
              (.send producer message)))
          (reify r/ResultProtocol
            (r/identifier [_] identifier)
            (r/expected [_] "messages sent")
            (r/result? [_] true)
            (r/actual [_] "?")
            (r/failures [_] nil))))

(defn make-queue-publisher [context queue-factory-string topic]
  (let [queue-factory (.lookup context queue-factory-string)
        queue-connection (.createConnection queue-factory)
        session (.createSession queue-connection
                             false
                             Session/AUTO_ACKNOWLEDGE)
        producer (.createProducer session (.lookup context topic))]
    (reify JMSAction
      (action [this args]
        (pub-action session producer args))
      (close-jms [this]
        (.close producer)
        (.close session)
        (.close queue-connection)))))

(defn make-topic-publisher [context topic-factory-string topic]
  (let [topic-factory (.lookup context topic-factory-string)
        topic-connection (.createConnection topic-factory)
        session (.createSession topic-connection
                                false
                                Session/AUTO_ACKNOWLEDGE )
        producer (.createProducer session (.lookup context topic))]
    (reify JMSAction
      (action [this args]
        (pub-action session producer args))
      (close-jms [this]
        (.close producer)
        (.close session)
        (.close topic-connection)))))

(defn consumer-action
  [session consumer args]
  (let [[identifier number timeout & regex] args
        to-test (partial exists-patterns regex)
        n (Integer/parseInt number)
        messages (until-all
                  (fn [msg]  (not (nil? msg)))
                  #(let [msg (.receive consumer 500)]
                     (if (not (nil? msg)) (.getText msg) nil))
                  n
                  (+ (System/currentTimeMillis)
                     (* 1000 (Integer/parseInt timeout))))
        results (map to-test messages)]
    (reify r/ResultProtocol
      (r/identifier [_] identifier)
      (r/expected [_] number)
      (r/result? [_] (and (= (count messages) n)
                          (exists-results results)))
      (r/actual [_] (count messages))
      (r/failures [_]
        (exists-failed-results results)))))

(defn make-queue-consumer [context queue-factory-string topic]
  (let [queue-factory (.lookup context queue-factory-string)
        queue-connection (.createConnection queue-factory)
        session (.createSession queue-connection
                                false
                                Session/AUTO_ACKNOWLEDGE)
        consumer (.createConsumer session
                                  (.lookup context topic))]
    (.start queue-connection)
    (reify JMSAction
      (action [this args]
        (consumer-action session consumer args))
      (close-jms [this]
        (.close consumer)
        (.close session)
        (.close queue-connection)))))

(defn make-topic-consumer [context topic-factory-string topic]
  (let [topic-factory (.lookup context topic-factory-string)
        topic-connection (.createConnection topic-factory)
        session (.createSession topic-connection
                                false
                                Session/AUTO_ACKNOWLEDGE)
        consumer (.createConsumer session (.lookup context
                                                   topic))]
    (.start topic-connection)
    (reify JMSAction
      (action [this args]
        (consumer-action session consumer args))
      (close-jms [this]
        (.close consumer)
        (.close session)
        (.close topic-connection)))))

(defn setup [m]
  (let [context (make-context (get m "context"))
        dest (get m "destinations")]
    (into {} (map #(condp = [(get % "type")
                           (get % "producer-consumer")]
                   ["topic"
                    "producer"] [["publish" (get % "destination")]
                                 (make-topic-publisher
                                  context
                                  (get % "factory")
                                  (get % "destination"))]
                    ["queue"
                     "producer"] [["publish" (get % "destination")]
                                  (make-queue-publisher
                                  context
                                  (get % "factory")
                                  (get % "destination"))]
                     ["topic"
                      "consumer"] [["consume" (get % "destination")]
                                   (make-topic-consumer
                                    context
                                    (get % "factory")
                                    (get % "destination"))]
                      ["queue"
                       "consumer"] [["consume" (get % "destination")]
                                    (make-queue-consumer
                                     context
                                     (get % "factory")
                                     (get % "destination"))])
                dest))))

(defaction pub-action-front
  "topic|file
  Publish on the given topic or queue name xml records from either
  a file or from a set of files in a directory."
  [& args]
  (let [[identifier _ producer-consumer topic & args] args]
    (apply action (vector
                       (get @connections [producer-consumer topic])
                       (concat [identifier] args)))))

(defaction consumer-action-front
  "topic|number|timeout(|regex)*
  Consume a number of messages, all of which must match at least
  one regex if set, until all are consumed. Timeout is in seconds."
  [& args]
  (let [[identifier _ producer-consumer topic & args] args]
    (apply action (vector
                            (get @connections [producer-consumer topic])
                            (concat [identifier] args)))))

(defn jms-service [m]
  (reify Service
    (initialize [_]
      (swap! connections (constantly (setup m))))
    (service-name [_] "jms")
    (services [_] {"publish" pub-action-front
                   "consume" consumer-action-front})
    ;; (perform-service [_ args]
    ;;   (let [[identifier _ producer-consumer topic & args] args]
    ;;     (apply action (vector
    ;;                    (get connections [producer-consumer topic])
    ;;                    (concat [identifier] args)))))
    (close [_] (doseq [[_ c] @connections]
                 (close-jms c)))))
