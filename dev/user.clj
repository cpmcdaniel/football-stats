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
            [football-stats.nfldotcom.storage :as storage]
            [football-stats.nfldotcom.api :as api]
            [football-stats.nfldotcom.schema :as schema]
            [football-stats.system :as sys]))

(def system nil)

(defn init
  "Constructs the current dev instance of the system."
  []
  (alter-var-root #'system (constantly (sys/system))))

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

(defn reset
  "Stops the system, refreshes all namespaces, and restarts it."
  []
  (stop)
  (refresh :after 'user/go))


