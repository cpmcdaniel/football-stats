(ns scratch
  (:require [football-stats.nfldotcom.storage :as storage]
            [football-stats.nfldotcom.schema :as schema]
            [football-stats.nfldotcom.player :as player]
            [clojure.java.io :as io]
            [clojure.pprint :refer [pprint]]
            [datomic.api :as d]
            [user :refer [go system]]))

(def datomic-uri "datomic:mem://football-stats")

(d/create-database datomic-uri)

(schema/ensure-migrations (d/connect datomic-uri))

(def conn (d/connect datomic-uri))


(comment
  (go)

  (def db (d/db (:conn system)))

  (defn as-entity [record]
    (d/entity db (first record)))

  (->
   (map as-entity
        (d/q '[:find ?g :where [?g :game/season 2009]
               [?g :game/week "REG1"]] db))

   first
   keys)

  (pprint (d/q '[:find ?pid :where [?p :player/name "T.Owens"]
                 [?p :player/nflid ?pid]] db))

  (player/get-player-profile "00-0012478"))
