(defproject bandit/kestrel "0.2.3-SNAPSHOT"
  :plugins [[lein-modules "0.3.11"]]
  :description "The Kestrel Module for Bandit"
  :source-paths ["src/clojure"]
  :java-source-paths ["src/java"]
  :profiles {:dev {:dependencies [[junit/junit "4.10"]]}}
  :dependencies [[org.slf4j/slf4j-api "1.6.4"]
                 [ch.qos.logback/logback-core "1.0.0"]
                 [ch.qos.logback/logback-classic "1.0.0"]
                 [com.twitter/finagle-kestrel "3.0.0"]
                 [bandit/core "_"]
                 [hiccup "_"]])
