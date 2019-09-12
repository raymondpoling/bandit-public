(defproject bandit/mongo "0.2.3-SNAPSHOT"
  :plugins [[lein-modules "0.3.11"]]
  :description "The Mongo Module for Bandit"
  :dependencies [[com.novemberain/monger "2.0.0"]
                 [clj-http "_"]
                 [bandit/core "_"]
                 [hiccup "_"]])
