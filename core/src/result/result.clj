(ns result.result
  (:use hiccup.core)
  (:import java.util.Date))

(defprotocol ResultProtocol
  (identifier [this])
  (expected [this])
  (result? [this])
  (actual [this])
  (failures [this]))

(defn text-printer [result]
  (if (satisfies? ResultProtocol result)
    (str (identifier result) ": "
         (expected result) " " (if (result? result)
                                 "Pass"
                                 "Fail")
         (if-not (result? result)
           (str "\nFailures: "
                (if (and (seq? (failures result))
                         (not (string? (failures result))))
                  (clojure.string/join " "
                                       (map #(str "[" % "]")
                                            (failures result)))
                  (failures result))
                "\nActual Results: "
                (if (and (seq? (actual result))
                         (not (string? (actual result))))
                  (apply str (actual result))
                  (actual result))) "\n"))
    (str "Running: " (clojure.string/join "|" result))))

(defn html-printer [result]
  (if (satisfies? ResultProtocol result)
    (let [result-string (if (result? result) "pass" "fail")]
      (str "<tr class=\"" result-string "\">"
           "<td>" (h (Date.)) "</td>"
           "<td>" (h (identifier result)) "</td>"
           "<td id=\"expected\">" (h (expected result)) "</td>"
           "<td id=\"actual\">" (h (actual result)) "</td>"
           "<td id=\"failure\">" (clojure.string/join
                                      "<br/>"
                                      (map h (failures result))) "</td>"
           "<td id=\"state\">" (h (.toUpperCase result-string)) "</td>"
           "</tr>"))
    (str "<tr><td class=\"running\" colspan=\"6\">"
         (h (clojure.string/join "|" result))
         "</td></tr>")))

(defn color-printer [result]
  (if (satisfies? ResultProtocol result)
    (str (identifier result) ": "
         (expected result) " " (if (result? result)
                                 "\u001B[32mPass\u001B[0m"
                                 "\u001B[31mFail\u001B[0m")
         (if-not (result? result)
           (str "\n\u001B[31mFailures: "
                (if (and (seq? (failures result))
                         (not (string? (failures result))))
                  (clojure.string/join " "
                                       (map #(str "[" % "]")
                                            (failures result)))
                  (failures result))
                "\nActual Results: "
                (if (and (seq? (actual result))
                         (not (string? (actual result))))
                  (apply str (actual result))
                  (actual result)) "\u001B[0m") "\n"))
    (str "Running: " (clojure.string/join "|" result))))

(defn choose-printer [printer]
  (cond
   (= printer "html") html-printer
   (= printer "color") color-printer
    :default text-printer))
