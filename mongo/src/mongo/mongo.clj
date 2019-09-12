(ns mongo.mongo
  (:require [monger.core :as mg]
            [monger.collection :as mc]
            [result.result :as r]
            [result.service :refer :all]
            [cheshire.core :refer :all])
  (:use clojure.data
        [util.util :only [until-all]]))

(def ^:dynamic *writeable* false)

(def ^:dynamic *connection* nil)

(defn set-connection! [host port db-user-pass-list]
  (alter-var-root (var *connection*)
                  (constantly (mg/connect {:host host :port port})))
  (doseq [item db-user-pass-list]
    (let [db (mg/get-db *connection* (get item "db"))
          user (get item "user")
          password (get item "password")]
      (mg/authenticate db user (.toCharArray password)))))

(defaction mongo-count
  "db|collection|expectation|timeout(|query)*
  Count a number of records in the collection of the given db.
  Expectation is the number that is expected to eventually appear
  in the db, while the query is one or more logically ORed expressions
  that will be used for counting. Timeout in seconds."
  [identifier _ _ db collection expectation timeout & others]
  (let [real-db (mg/get-db *connection* db)
        queries (map parse-string others)
        expected (Integer/parseInt expectation)
        timeout-secs (* 1000 (Integer/parseInt timeout))
        result (count
                (until-all
                 (fn [count] (= count expected))
                 (fn [] (mc/count real-db collection {:$or queries}))
                 5
                 (+ (System/currentTimeMillis) timeout-secs)))]
    (reify r/ResultProtocol
      (r/identifier [_] identifier)
      (r/expected [_] expectation)
      (r/result? [_] (= 5 result))
      (r/actual [_] result)
      (r/failures [_] [(str "Actual " result " not 5.")]))))

(defaction mongo-exists
  "db|collection|query|max-wait
  This action waits for a single document matching the query to appear.
  It is primarily used for proving that a given document appears well
  formed after having been processed by system processes. The query
  will be polled repeatedly until about the max-wait time expires,
  at which time it will be assumed that the test has failed."
  [identifier _ _ db collection query wait & _]
  (let [real-db (mg/get-db *connection* db)
        json-query (parse-string query)
        verification #(mc/find-one-as-map real-db
                                          collection json-query {:_id 1})]
    (loop [current-wait (* 20 (Integer/parseInt wait))
           ready? (verification)]
      (if-not (or (>= 0 current-wait) ready?)
        (do
          (Thread/sleep 50)
          (recur (- current-wait 1) (verification)))
        (reify r/ResultProtocol
          (r/identifier [_] identifier)
          (r/expected [_] query)
          (r/result? [_] ready?)
          (r/actual [_] ready?)
          (r/failures [_] ["Record did not exist."]))))))

(defaction mongo-compare
  "db|collection|query1|query2(|excludes)
  Compares two documents found by the queries. As some fields,
  such as _id or processing time stamps, they can be excluded by listing
  them pipe delimitted in the excludes section."
  [identifier _ _ db collection query1 query2 & excludes]
  (let [real-db (mg/get-db *connection* db)
        query1-json (parse-string query1)
        query2-json (parse-string query2)
        exclude-keys (map keyword excludes)
        results (mc/find-maps real-db collection {:$or [query1-json
                                                        query2-json]})
        res (map #(apply dissoc (cons % exclude-keys)) results)
        [left right common] (diff (first res)
                                  (second res))]
    (reify r/ResultProtocol
      (r/identifier [_] identifier)
      (r/expected [_] "=")
      (r/result? [_]
        (and (= 2 (count results))
             (nil? left)
             (nil? right)))
      (r/actual [_] results)
      (r/failures [this] (if-not (r/result? this) [left right] [])))))

(defaction mongo-remove
  "db|collection(|query)*
  Remove documents from the given db and collection. Query expressions
  may be used to control which documents are removed, pipe delimitted
  at the end of the line."
  [identifier _ _ db collection & others]
  (let [real-db (mg/get-db *connection* db)
        queries (map parse-string others)
        result (if *writeable*
                 (mc/remove real-db collection {:$or queries})
                 false)]
    (if *writeable*
      (loop [c (mc/count real-db collection {:$or queries})]
        (if (> c 0)
          (do
            (Thread/sleep 50)
            (recur (mc/count real-db collection {:$or queries}))))))
    (reify r/ResultProtocol
      (r/identifier [_] identifier)
      (r/expected [_] "removed")
      (r/result? [_] *writeable*)
      (r/actual [_] result)
      (r/failures [_] [(str "Writeable is " *writeable*)]))))

(defn mongo-service [m]
  (reify Service
    (initialize [_] (let [host (get m "host")
                          port (get m "port")
                          db-user-pass-list (get m "db-user-pass")
                          writeable? (get m "writeable")]
                      (if writeable?
                        (alter-var-root (var *writeable*)
                                        (constantly writeable?)))
                      (set-connection! host port db-user-pass-list)))
    (service-name [_] "mongo")
    (services [_] {"count" mongo-count
                   "compare" mongo-compare
                   "exists" mongo-exists
                   "remove" mongo-remove})
    ;; (perform-service [_ args]
    ;;   (apply mongo-action args))
    (close [_] (mg/disconnect *connection*))))
