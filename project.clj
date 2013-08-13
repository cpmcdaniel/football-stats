(defproject football-stats "0.1.0-SNAPSHOT"
  :description "Football stats aggregator and analyzer"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/tools.logging "0.2.6"]
                 [clj-http "0.7.2"]
                 [clj-webdriver "0.6.0"]
                 [cheshire "5.2.0"]
                 [com.datomic/datomic-free "0.8.4122"]
                 [retry "1.0.2"]
                 [lamina "0.5.0-rc3"]
                 [org.slf4j/slf4j-log4j12 "1.7.5"]]
  :exclusions [org.slf4j/slf4j-nop
               org.slf4j/log4j-over-slf4j]
  :profiles {:dev {:source-paths ["dev"]
                   :dependencies [[org.clojure/tools.namespace "0.2.3"]
                                  [org.clojure/java.classpath "0.2.0"]]}}
  :checksum :warn
  ;;:main football-stats.core

  :jvm-opts ["-Xmx1g"]
  )
