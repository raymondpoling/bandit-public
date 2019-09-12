(ns websocket.websocket
  (:require [lamina.core :as ch]
            [result.result :as r]
            [result.service :refer :all]
            [aleph.http.websocket :as websocket])
  (:use [result.service]
        [util.util]))

(def aliases (atom {}))

(def active (atom {}))

;; actions:
;; open -> create websocket (via alias), with handshake.
;; close -> takes alias, but auto via Service also
;; send -> send messages
;; recieve -> Receives X messages within timeout period (timeout renewed
;;   per message).
;;
;; config:
;; alias -> names first part of endpoint
;; ignorable -> messages to ignore (like heartbeats)

(defaction open-websocket
  "alias|endpoint|handshake
  Open a websocket to the server described in alias at the specified
  endpoint. Send the handshake message to start processing."
  [identifier _ _ alias endpoint handshake & _]
  (let [maybe (get @aliases alias)
        url (if (= nil maybe) endpoint (str maybe endpoint))
        channel @(websocket/websocket-client {:url url})]
    (swap! active #(assoc % alias channel))
    (ch/enqueue channel handshake))
  (reify r/ResultProtocol
    (r/identifier [_] identifier)
    (r/expected [_] "connected")
    (r/result? [_] true)
    (r/actual [_] "connected")
    (r/failures [_] nil)))

(defaction close-websocket
  "alias
  Close the endpoint specified by alias."
  [identifier _ _ alias & _]
  (let [channel (get @active alias)]
    (ch/close channel)
    (reify r/ResultProtocol
      (r/identifier [_] identifier)
      (r/expected [_] "closed")
      (r/result? [_] true)
      (r/actual [_] "closed")
      (r/failures [_] nil))))

(defaction send-message
  "alias|message
  Send a message over the websocket connection."
  [identifier _ _ alias message & _]
  (let [channel (get @active alias)]
    (ch/enqueue channel message)
    (reify r/ResultProtocol
      (r/identifier [_] identifier)
      (r/expected [_] "Pass")
      (r/result? [_] true)
      (r/actual [_] "Pass")
      (r/failures [_] nil))))

(defn receive-message
  "alias|number|wait(|regex)*
  Receive a given number of messages within a given wait period
  in seconds. Messages must match at least one of a set of regex
  patterns."
  [identifier _ _ alias number wait & patterns]
  (let [pats (partial exists-patterns patterns)
        channel (get @active alias)
        results (doall
                 (map
                  (fn [_] (let [message (try (ch/wait-for-message
                                             channel
                                             (* 1000 (Integer/parseInt wait)))
                                            (catch Exception e nil))]
                           (if (nil? message)
                             [nil nil]
                             (pats message))))
                  (range (Integer/parseInt number))))]
    (reify r/ResultProtocol
      (r/identifier [_] identifier)
      (r/expected [_] number)
      (r/result? [_] (exists-results results))
      (r/actual [_] results)
      (r/failures [_] (exists-failed-results results)))))

(defn websocket-service [conf]
  (reify Service
    (initialize [_] (swap! aliases #(merge % conf)))
    (service-name [_] "websocket")
    (services [_] {"open" open-websocket,
                   "close" close-websocket,
                   "send" send-message,
                   "receive" receive-message})
    ;; (perform-service [_ args]
    ;;   (let [[init _ action & args] args]
    ;;     (apply (condp = action
    ;;              "open" open-websocket
    ;;              "close" close-websocket
    ;;              "send" send-message
    ;;              "receive" receive-message)
    ;;            (cons init args))))
    (close [_] (try (doseq [[name i]  @active]
                   (ch/close i))
                    (catch Exception e (println "Caught an exception:"
                                                (.printStackTrace e)))))))
