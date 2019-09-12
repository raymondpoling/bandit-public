(defproject bandit/jms "0.2.3-SNAPSHOT"
  :plugins [[lein-modules "0.3.11"]]
  :description "The JMS Module for Bandit"
  :dependencies [[javax.jms/jms "1.1"]
                 [org.apache.activemq/activemq-all "5.10.0"]
                 [bandit/core "_"]
                 [hiccup "_"]]
  :repositories [["jboss"
                  {:url
                   "http://repository.jboss.org/nexus/content/groups/public/"}]
                 ])
