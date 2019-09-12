(ns xml.file-consumer-test
  (:require [xml.file-consumer :refer :all]
            [clojure.test :refer :all]))

(deftest detect-xml-test
  (testing "If we have xml, it is detected."
    (is (detect-xml "<is xml!>"))
    (is (detect-xml "     <also xml!"))
    (is (detect-xml "\t<me too")))
  (testing "If it isn't xml, we don't detect is."
    (is (not (detect-xml "")))
    (is (not (detect-xml "|kitty<")))))

(deftest detect-properties-test
  (testing "If we have properties, we detect them."
    (is (detect-properties "|property|1"))
    (is (detect-properties "\t|also a property"))
    (is (detect-properties "     |also a property")))
  (testing "If it isn't properties, don't detect them."
    (is (not (detect-properties "not a property|1")))
    (is (not (detect-properties "\tnot a property|")))))

(deftest detect-empty-test
  (testing "We detect empty lines."
    (is (detect-empty ""))
    (is (detect-empty "\t\t\t"))
    (is (detect-empty "      ")))
  (testing "We do not detect non-empty lines."
    (is (not (detect-empty "cat")))
    (is (not (detect-empty "  \t\t  d")))))

(deftest read-xml-test
  (testing "We will read several lines of xml as required."
    (is (= [["<xml","xml","xml>"],["",""]]
           (read-xml ["<xml","xml","xml>","",""])))))

(deftest read-properties-test
  (testing "We will read several lines of xml as required."
    (is (= [["|xml","xml","xml|"],["",""]]
           (read-properties ["|xml","xml","xml|","",""])))))

(deftest drop-empty-test
  (testing "We will read several lines of xml as required."
    (is (= ["|xml","xml","xml|"]
           (discard-empty-lines ["","","|xml","xml","xml|"])))))

(deftest read-messages-test
  (testing "Given a sequence of xml, properties, and spaces, we get back desired output."
    (let [template [["<this starts us up","more stuff","yet more","ends"],
                    ["",""]
                    ["|properties1","|props2","props"]
                    ["",""]
                    ["<more xml","done>"]
                    ["",""]
                    ["<last of xml, no props","done>"]
                    ["",""]]
          expected [["<this starts us upmore stuffyet moreends",
                     [[ "properties1"],["props2props"]]]
                    ["<more xmldone>" []]
                    ["<last of xml, no propsdone>" []]]
          input  (reduce concat template)]
      (is (= expected
             (message-seq input))))))
