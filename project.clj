(defproject bandit "0.2.3-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

   :plugins [[lein-tar "3.2.0"]
             [lein-pprint "1.1.1"]
             [lein-modules "0.3.11"]]
   :tar {:uberjar true
         :format :tgz}
   :main ^:skip-aot bandit.core
   :target-path "target/%s"
   :profiles {:uberjar {:aot :all}
              ;; :profiles {:provided
              ;;            {:dependencies
              ;;             [[bandit/core :version :scope "runtime"]]

              ;; }}
              }

   :release-tasks [["vcs" "assert-committed"]
                   ["change" "version" "leiningen.release/bump-version" "release"]
                   ["modules" "change" "version" "leiningen.release/bump-version" "release"]
                   ["vcs" "commit"]
                   ["vcs" "tag" "--no-sign"]
                   ["modules" "install"]
                   ["uberjar"]
                   ["change" "version" "leiningen.release/bump-version"]
                   ["modules" "change" "version" "leiningen.release/bump-version"]
                   ["vcs" "commit"]
                   ["vcs" "push"]]
   :modules  {:inherited
              {
               :aliases      {"all" ^:displace ["do" "clean," "test," "install"]
                              "-f" ["with-profile" "+fast"]}
               :dependencies [[org.clojure/clojure "_"]]
               :aot :all
               }
              :versions {org.clojure/clojure "1.6.0"
                         clj-http "0.9.2"
                         hiccup "1.0.5"
                         core :version}}
   :dependencies [[bandit/jms :version]
                  [bandit/mongo :version]
                  [bandit/shell :version]
                  [bandit/rest :version]
                  [bandit/websocket :version]
                  [bandit/core :version]
                  [bandit/kestrel :version]])
