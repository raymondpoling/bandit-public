(ns util.util)

(defn and-fn
  ([] true)
  ([x] x)
  ([x y] (and x y)))

(defn or-fn
  ([] true)
  ([x] x)
  ([x y] (or x y)))

(defn forall-patterns [patterns to-test]
  (let [pats (map #(vector (re-pattern %) %) patterns)
        result (map #(vector (re-find (first %) to-test) (second %)) pats)]
    [(map second (filter #(nil? (first %)) result)) to-test]))

(defn exists-patterns [patterns to-test]
  (let [pats (map re-pattern patterns)]
    [(reduce or-fn (map #(re-find % to-test) pats)) to-test]))

(defn forall-results [outs]
  (reduce and-fn (map #(= 0 (count (first %))) outs)))

(defn exists-results [outs]
  (reduce and-fn (map #(not (empty? (first %))) outs)))

(defn forall-failed-results [outs]
  (filter #(not (empty? (first %))) outs))

(defn exists-failed-results [outs]
  (map second (filter #(or (= true (first %)) (empty? (first %))) outs)))

(defn until-all ([expectation func counts timeout]
                   (until-all expectation func counts '() timeout))
  ([expectation func counts current timeout]
     (let [value (func)
           success (expectation value)
           totally-done (if success (= counts (count (cons value current)))
                            (= counts (count current)))
           current-time (System/currentTimeMillis)
           timedout (> current-time timeout)]
       (cond
           (and success totally-done) (cons value current)
           timedout current
           success (do
                     (Thread/sleep 500)
                     (until-all expectation func counts (cons value current) timeout))
           :default (do
                      (Thread/sleep 500)
                      (until-all expectation func counts current timeout))))))
