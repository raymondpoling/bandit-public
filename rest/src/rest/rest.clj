(ns rest.rest
  (:require [result.result :as r]
            [clj-http.client :as client]
            [cheshire.core :refer :all]
            [result.service :refer :all])
  (:use [result.service]
        util.util))

(def aliases (atom {}))

(defn rest-action [args]
  (let [[identifier _ method alias url headers body code & regex] args
        header-file (if (> (count headers) 0) (clojure.string/split
                                               (slurp headers)
                                               #"\n")
                        "")
        http-headers (if (> (count header-file) 0)
                       (into {} (map (fn [a] [(keyword (clojure.string/trim
                                                       (first a)))
                                        (clojure.string/trim (second a))])
                                     (map #(clojure.string/split % #":" 2)
                                          header-file)))
                       {})
        maybe (get @aliases alias)
        unaliased-url (if (= nil maybe) url (str maybe url))
        proc-url (if (= \@ (first url))
                   (with-open [o (clojure.java.io/reader
                                  (clojure.java.io/file
                                   (subs unaliased-url 1)))]
                     (first (line-seq o)))
                   unaliased-url)
        request {:method (keyword method)
                 :url proc-url
                 :throw-exceptions false
                 :headers (merge http-headers {:content-type "application/json"})
                 :as :json
                 :coerce :always}
        response (client/request
                  (if (not (= body ""))
                    (merge {:body body} request)
                    request))
        tests (partial forall-patterns regex)
        results [(tests (generate-string (:body response)))]]
    (reify r/ResultProtocol
      (r/identifier [_] identifier)
      (r/expected [_] (str "Url:" proc-url "Code: " code " Regex: " regex))
      (r/result? [_] (and (= (Integer/parseInt code) (:status response))
                          (forall-results results)))
      (r/actual [_] (generate-string (:body response)))
      (r/failures [_] (cons (str "Code was: " (:status response) " excpected: " code)  (forall-failed-results results))))))

(defaction rest-get
  "alias|url|headers|body|code(|regex)*
  Get the requested resource, either at the alias + url, or at the url.
  The body is properly parsed JSON, and the code is the expected return
  code (200 for OK, 500 for Internal Server Error, etc.
  One or more regex patterns broken up with | may be specified, of which
  all must match.
  Headers is a file of headers, in standard \"key: value\" format."
  [& args]
  (rest-action args))

(defaction rest-put
  "alias|url|headers|body|code(|regex)*
  Put the requested resource, either at the alias + url, or at the url.
  The body is properly parsed JSON which will be sent to the server to,
  be put, and the code is the expected return code (200 for OK, 500 for
  Internal Server Error, etc.
  One or more regex patterns broken up with | may be specified, of which
  all must match.
  Headers is a file of headers, in standard \"key: value\" format."
  [& args]
  (rest-action args))

(defaction rest-post
  "alias|url|headers|body|code(|regex)*
  Post the requested resource, either at the alias + url, or at the url.
  The body is properly parsed JSON which will be sent to the server to,
  be put, and the code is the expected return code (200 for OK, 500 for
  Internal Server Error, etc.
  One or more regex patterns broken up with | may be specified, of which
  all must match.
  Headers is a file of headers, in standard \"key: value\" format."
  [& args]
  (rest-action args))

(defaction rest-delete
  "alias|url|headers|body|code(|regex)*
  Delete the requested resource, either at the alias + url, or at the url.
  The body is properly parsed JSON which will be sent to the server to,
  be put, and the code is the expected return code (200 for OK, 500 for
  Internal Server Error, etc.
  One or more regex patterns broken up with | may be specified, of which
  all must match.
  Headers is a file of headers, in standard \"key: value\" format."
  [& args]
  (rest-action args))

(defn rest-service [conf]
  (reify Service
    (initialize [this] (swap! aliases #(merge % conf)))
    (service-name [this] "rest")
    (services [this] {"get" rest-get,
                      "put" rest-put,
                      "post" rest-post,
                      "delete"rest-delete})
    ;; (perform-service [this args]
    ;;   (rest-action args))
    (close [_] nil)))
