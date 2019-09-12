(ns util.util-test
  (:require [clojure.test :refer :all]
            [util.util :refer :all]))
(deftest and-fn-test
  (testing "Nothing is true."
    (is (and-fn)))
  (testing "True is true."
    (is (and-fn true)))
  (testing "False is false."
    (is (= false (and-fn false))))
  (testing "True and True is True."
    (is (and-fn true true)))
  (testing "False and True is False."
    (is (= false (and-fn false true))))
  (testing "True and False is False."
    (is (= false (and-fn true false))))
  (testing "False and False is False."
    (is (= false (and-fn false false)))))

(deftest forall-patterns-test
  (let [patterns ["cat" "dog" "mouse"]
        and-pat-test (partial forall-patterns patterns)]
    (testing "No patterns it passes."
      (is (= [[] "cat"] (forall-patterns [] "cat"))))
    (testing "Matches all patterns, it passes."
      (let [test-pat "The cat, dog and mouse are friends."]
        (is (= [[] test-pat] (and-pat-test test-pat)))))
    (testing "Fails first pattern, get first pattern matched with message."
      (let [test-pat "We just have a dog and a mouse."]
        (is (= [["cat"] test-pat] (and-pat-test test-pat)))))
    (testing "Third pattern fails."
      (let [test-pat "The cats and dogs fight, but that's it."]
        (is (= [["mouse"] test-pat] (and-pat-test test-pat)))))
    (testing "All patterns fail."
      (let [test-pat "grr"]
        (is (= [["cat" "dog" "mouse"] test-pat] (and-pat-test test-pat)))))))

(deftest forall-results-test
  (testing "A list where all lengths are zero."
    (is (= true (forall-results [[[] "cat"] [[] "dog"] [[] "mouse"]]))))
  (testing "First one isn't zero."
    (is (= false (forall-results [[["cat"] "cat"] [[] "dog"] [[] "mouse"]]))))
  (testing "Last one is zero."
    (is (= false (forall-results [[[] "cat"] [[] "dog"] [["cat"] "mouse"]]))))
  (testing "All aren't zero."
    (is (= false (forall-results [[["cat"] "cat"] [["dog" "woof"] "dog"] [["mouse"] "mouse"]])))))

(deftest forall-failed-results-test
  (testing "A list where all lengths are zero."
    (is (= [] (forall-failed-results [[[] "cat"] [[] "dog"] [[] "mouse"]]))))
  (testing "First one isn't zero."
    (is (= [[["cat"] "cat"]]
           (forall-failed-results [[["cat"] "cat"] [[] "dog"] [[] "mouse"]]))))
  (testing "Last one is zero."
    (is (= [[["cat"] "mouse"]]
           (forall-failed-results [[[] "cat"] [[] "dog"] [["cat"] "mouse"]]))))
  (testing "All aren't zero."
    (is (= [[["cat"] "cat"] [["dog" "woof"] "dog"] [["mouse"] "mouse"]]
           (forall-failed-results
            [[["cat"] "cat"] [["dog" "woof"] "dog"] [["mouse"] "mouse"]])))))

(deftest or-fn-test
  (testing "Nothing is true."
    (is (or-fn)))
  (testing "True is true."
    (is (or-fn true)))
  (testing "False is false."
    (is (= false (or-fn false))))
  (testing "True and True is True."
    (is (or-fn true true)))
  (testing "False and True is True."
    (is (or-fn false true)))
  (testing "True and False is True."
    (is (or-fn true false)))
  (testing "False and False is False."
    (is (= false (or-fn false false)))))

(deftest exists-patterns-test
  (let [patterns ["cat" "dog" "mouse"]
        or-pat-test (partial exists-patterns patterns)]
    (testing "No patterns it passes."
      (is (= [true "cat"] (exists-patterns [] "cat"))))
    (testing "Matches all patterns, it passes."
      (let [test-pat "The cat, dog and mouse are friends."]
        (is (= ["cat" test-pat] (or-pat-test test-pat)))))
    (testing "Fails first pattern, the second pattern matches with message."
      (let [test-pat "We just have a dog and a mouse."]
        (is (= ["dog" test-pat] (or-pat-test test-pat)))))
    (testing "Third pattern fails, still passes."
      (let [test-pat "The cats and dogs fight, but that's it."]
        (is (= ["cat" test-pat] (or-pat-test test-pat)))))
    (testing "All patterns fail."
      (let [test-pat "grr"]
        (is (= [nil test-pat] (or-pat-test test-pat)))))))

(deftest exists-results-test
  (testing "A list where all are not-nil."
    (is (= true (exists-results [["cat" "cat"] ["dog" "dog"] ["mouse" "mouse"]]))))
  (testing "First one is a failure."
    (is (= false (exists-results [[nil "cat"] ["dog" "dog"] ["mouse" "mouse"]]))))
  (testing "Last one is zero."
    (is (= false (exists-results [["cat" "cat"] ["dog" "dog"] [nil "mouse"]]))))
  (testing "All aren't zero."
    (is (= false (exists-results [[nil "cat"] [nil "dog"] [nil "mouse"]])))))

(deftest exists-failed-results-test
  (testing "A list where all are not-nil."
    (is (= [] (exists-failed-results
                 [["cat" "cat"] ["dog" "dog"] ["mouse" "mouse"]]))))
  (testing "First one is a failure."
    (is (= ["cat"] (exists-failed-results
                  [[nil "cat"] ["dog" "dog"] ["mouse" "mouse"]]))))
  (testing "Last one is zero."
    (is (= ["mouse"] (exists-failed-results
                  [["cat" "cat"] ["dog" "dog"] [nil "mouse"]]))))
  (testing "All aren't zero."
    (is (= ["cat" "dog" "mouse"] (exists-failed-results
                  [[nil "cat"] [nil "dog"] [nil "mouse"]])))))
