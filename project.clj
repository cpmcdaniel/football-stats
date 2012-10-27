(defproject football-stats "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.0-alpha4"]
                 [clj-http "0.5.6"]
                 [clj-webdriver "0.6.0-alpha11"]
                 [cheshire "4.0.3"]
                 [com.datomic/datomic-free "0.8.3551"]
                 [retry "1.0.2"]]
  :profiles {:dev {:source-paths ["dev"]
                   :dependencies [[org.clojure/tools.namespace "0.2.0"]]}}
  :checksum :warn
  ;;:main football-stats.core

  ;;  Project Notes
  ;;
  ;;  TODO
  ;;  * Unit test for storing game home team rushing
  ;;  * Store all games in Datomic.
  )