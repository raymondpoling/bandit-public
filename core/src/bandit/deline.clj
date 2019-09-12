(ns bandit.deline
  (:require [cheshire.core :refer :all]))

(defn get-lines [fname]
  (try
    (->>
     fname
     (filter #(not (re-find #"^\s*$" %)))
     (map #(let [t (re-find #"^#.*" %)]
            (if-not t
              %
              (str "|comment|comment|" %)))))

    (catch Exception e
      (.printStackTrace e))))

(defn split-line [line]
  (let [pattern #"\|"
        temp (clojure.string/replace line "\\|" "%7C")
        split-up (clojure.string/split temp pattern)
        post-proc (comp clojure.string/trim
                        (fn [l] (clojure.string/replace l "%7C" "|")))]
    (map post-proc split-up)))

(defn get-split-file-lines [fname]
  (map split-line (get-lines fname)))
