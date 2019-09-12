(defproject bandit/core "0.2.3-SNAPSHOT"
  :plugins [[lein-modules "0.3.11"]]
  :description "The Core Module for Bandit"
  :dependencies [[hiccup "_"]
                 [clj-http "_"]]
  :main ^:skip-aot bandit.core)
