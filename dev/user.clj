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

(def nflgame (read-string (slurp "test/data/2011090800")))

(def uri "datomic:mem://test")
(def conn (do 
            (d/delete-database uri)
            (d/create-database uri)
            (d/connect uri)))
(def mydb (do
            (install conn)
            (db conn)))


