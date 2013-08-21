(ns user
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.pprint :refer (pprint)]
            [clojure.repl :refer :all]
            [clojure.test :as test]
            [clojure.tools.logging :as log]
            [clojure.tools.namespace.repl :refer (refresh refresh-all)]
            [clj-webdriver.taxi :as taxi]
            [clj-http.client :as http]
            [datomic.api :refer [db q] :as d]
            [lamina.core :as l]
            [football-stats.nfldotcom.storage :as storage]
            [football-stats.nfldotcom.api :as api]
            [football-stats.nfldotcom.schema :as schema]
            [football-stats.system :as sys]))

(def system nil)

(defn init
  "Constructs the current dev instance of the system."
  []
  (alter-var-root #'system (constantly (sys/system)
                                       #_{:status :initialized})))

(defn start
  "Starts the current dev system."
  []
  (alter-var-root #'system sys/start))

(defn stop
  "Stops the current dev system."
  []
  (alter-var-root #'system
                  (fn [s] (when s (sys/stop s)))))

(defn go
  []
  "Initializes the current dev system and starts it."
  (init)
  (start))

(defn play
  []
  (alter-var-root #'system (constantly {:datomic-uri "datomic:mem://football-stats"
                                        :status :initialized}))
  (start))

(defn reset
  "Stops the system, refreshes all namespaces, and restarts it."
  []
  (stop)
  (refresh :after 'user/go))

(defn resetp
  "Stops the system, refreshes all namespaces, and restarts it."
  []
  (stop)
  (refresh :after 'user/play))

(defn scrape-week [season week]
  (let [browser (taxi/new-driver {:browser :firefox})]
    (l/enqueue (:week-channel system)
               {:season season
                :week week
                :web-driver browser})
    browser))

(defn download-games []
  (go)
  (let [driver (taxi/new-driver {:browser :firefox})
        download (:download-seasons system)]
    (download driver 2009 2010 2011 2012)))
