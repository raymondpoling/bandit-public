(ns result.service
  (:require [result.result :as r]))

(defprotocol Service
  (initialize [this])
  (service-name [this])
  (services [this])
  ;; (perform-service [this args])
  (close [this]))

(defmacro defaction [name docstring args & body]
  `(def ~name ^{:doc ~docstring} (fn ~args ~@body)))

(defn perform-service ^:r/Result [^:Service service args]
  (let [[identity _ action & _] args]
    (apply (get (services service) action) args)))

(defn documentation [^:Service service]
  (println "Service:" (service-name service))
  (doseq [[action action-fn] (services service)]
    (println action (:doc (meta action-fn)))))


(defmacro def-service [name docstring {:keys [actions close initialize]}]
  `(defn ~(symbol (str name "-service")) [~'conf]
     ~(if initialize
        `(~@(cons 'fn initialize) ~'conf))
     (reify Service
       (service-name [_] ~'name)
       (services [_] ~(map #(vector (first %) (second %)) actions))
       (perform-service [_ args#]
         (let [[ident# _ action# & rest#] args#]
           (let [actions# (into {}
                                (map
                                 (fn [~'i] [(first ~'i) (nth ~'i 3)]) ~@actions))]
             (apply (get actions# action#)
                    (cons ident# rest#)))))
       (close [_] ~@close))))

(defn failed-result [service-name]
  (reify Service
    (service-name [this] service-name)
    (services [this] (reify java.util.Map
                       (get [self _key]
                         (fn [& args]
                           (reify r/ResultProtocol
                             (r/identifier [_] (str "No such service " service-name))
                             (r/expected [_] "service exists")
                             (r/result? [_] false)
                             (r/actual [_] (str "No such service " service-name))
                             (r/failures [_] (str "No such service " service-name)))))))
    (close [this])))
