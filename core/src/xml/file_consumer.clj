(ns xml.file-consumer)

(defn lines [file]
  (line-seq (clojure.java.io/reader file)))

(defn detect-xml [line]
  (if-not (nil? line)
    (re-find #"^\s*<" line)
    nil))

(defn detect-empty [line]
  (if-not (nil? line)
    (re-find #"^\s*$" line)
    nil))

(defn detect-properties [line]
  (if-not (nil? line)
    (re-find #"^\s*\|" line)
    nil))

(defn read-xml [lines]
  (letfn [(xml-lines [acc lines]
            (if (not (detect-empty (first lines)))
              (recur (conj acc (first lines)) (rest lines))
              [acc lines]))]
    (xml-lines [] lines)))

(defn read-properties [lines]
  (letfn [(prop-lines [acc lines]
            (if (not (detect-empty (first lines)))
              (recur (conj acc (first lines)) (rest lines))
              [acc lines]))]
    (prop-lines [] lines)))

(defn discard-empty-lines [lines]
  (if (detect-empty (first lines))
    (recur (rest lines))
    lines))

(defn message-seq [lines]
  (lazy-seq
   (let [start (drop-while detect-empty lines)
         [xml tl1] (split-with #(not (detect-empty %)) start)
         remove-blanks (drop-while detect-empty tl1)
         maybe-properties? (detect-properties (first remove-blanks))
         [properties tl2] (if maybe-properties?
                            (split-with #(not (detect-empty %)) remove-blanks)
                            [[] remove-blanks])
         processed-props (map
                          #(clojure.string/split % #"=")
                          (filter #(not (= "" %))
                                  (clojure.string/split
                                   (clojure.string/join "" properties)
                                   #"\|")))]
     (cons [(clojure.string/join "" xml)
            processed-props] (if (empty? tl2)
                               ()
                               (message-seq tl2))))))

(defn read-file-or-files [filename]
  (let [file (clojure.java.io/file filename)]
    (filter #(not (empty? (clojure.string/trim (first %))))
            (reduce concat (map #(message-seq (lines %))
                                (filter #(not (.isDirectory %))
                                        (file-seq file)))))))
