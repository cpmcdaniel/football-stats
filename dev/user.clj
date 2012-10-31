(ns user
  (:require [clj-webdriver.taxi :as taxi]
            [clj-http.client :as http]
            football-stats.nfldotcom.storage-test
            football-stats.nfldotcom.api-test)
  (:use clojure.tools.namespace.repl
        [clj-http.util :only [url-encode]]
        clojure.test
        [datomic.api :only [db q] :as d]
        football-stats.nfldotcom.storage
        football-stats.nfldotcom.api
        [football-stats.nfldotcom.schema :as schema]))

