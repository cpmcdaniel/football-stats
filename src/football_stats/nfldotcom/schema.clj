(ns football-stats.nfldotcom.schema
  (:use [datomic.api :only [q db] :as d]
        football-stats.nfldotcom.storage
        [clojure.java.io :only [resource]]
        [cheshire.core :only [parse-string]]))

(defn get-static-team-info []
  (parse-string
   (slurp
    (resource "football_stats/nfldotcom/team-data.json"))
   true))

(defn create-metadata-txs []
  ;; Transaction data for team info.
  (for [[abbr team] (get-static-team-info)]
    {:db/id (d/tempid :db.part/user)
     :team/abbr (:abbr team)
     :team/name (:city team)
     :team/mascot (:nickname team)}))

(defn install-metadata [conn]
  (do
    (d/transact conn (create-metadata-txs))))

(defn install-schema [conn]
  (d/transact
   conn
   (read-string
    (slurp
     (resource "football_stats/nfldotcom/schema.dtm")))))

(defn install [conn]
  (do
    (install-schema conn)
    (install-metadata conn)))


(defn test-db []
  (let [uri "datomic:mem://test"
        conn (do 
               (d/delete-database uri)
               (d/create-database uri)
               (d/connect uri))
        mydb (do
               (install conn)
               (db conn))]
    (d/touch (d/entity mydb (first (first (q '[:find ?t :where [?t :team/abbr "GB"]] mydb)))))))

(comment
  ;; Example game entity.
  {:db/id 1
   :game/gameid "2011090800"
   :game/nflraw "{...JSON DATA...}"
   :game/season 2012
   :game/week :REG1
   :game/date "2011-09-08" ; :db.type/instant
   :home/team {:db/id 2
               :team/abbr "GB"
               :team/name "Green Bay"
               :team/mascot "Packers"}
   :home/score {:db/id 3
                :score/q1 7
                :score/q2 7
                :score/q3 3
                :score/q4 10
                :score/ot 0
                :score/final 27}
   :home/passing #{{:db/id 6
                    :stats/player {:db/id 7
                                   :player/nflid "00-7654321"
                                   :player/name "A.Rodgers"
                                   :player/position :QB
                                   :player/current-team {}}
                    :passing/attempts 20
                    :passing/complete 18
                    :passing/yards 350
                    :passing/tds 3
                    :passing/ints 0
                    :passing/twopta 0
                    :passing/twoptm 0}}
   :home/rushing #{{:db/id 4
                    :stats/player {:db/id 5
                                   :player/nflid "00-1234567"
                                   :player/name "C.Benson"
                                   :player/position :RB
                                   :player/current-team {}}
                    :rushing/yards 100
                    :rushing/attempts 20
                    :rushing/tds 0
                    :rushing/long 19
                    :rushing/longtd 0
                    :rushing/twopta 0
                    :rushing/twoptm 0}}
   :home/receiving #{{:db/id 8
                      :stats/player {:db/id 9
                                     :player/nflid "00-1111111"
                                     :player/name "J.Finley"
                                     :player/position :TE
                                     :player/current-team {}}
                      :receiving/receptions 3
                      :receiving/yards 53
                      :receiving/tds 0
                      :receiving/long 20
                      :receiving/longtd 0
                      :receiving/twopta 0
                      :receiving/twoptm 0}}
   :home/kicking #{{:db/id 10
                    :stats/player {:db/id 11
                                   :player/nflid "00-2222222"
                                   :player/name "M.Crosby"
                                   :player/position :K
                                   :player/current-team {}}
                    :kicking/fg-attempts 0
                    :kicking/fg-made 0
                    :kicking/xp-attempts 6
                    :kicking/xp-made 6}}
   :home/defense #{{:db/id 12
                    :stats/player {}
                    :defense/sacks 0.5
                    :defense/ints 1
                    :defense/tackles 2
                    :defense/assists 1
                    :defense/fumbles-forced 0}}
   :home/fumbles #{{:db/id 13
                    :stats/player {}
                    :fumbles/lost 1
                    :fumbles/recovered 0
                    :fumbles/total 1
                    :fumbles/trcv 0}}
   :home/puntret #{{:db/id 14
                    :stats/player {}
                    :puntret/returns 2
                    :puntret/tds 1
                    :puntret/long 72
                    :puntret/avg 46
                    :puntret/longtd 72}}
   :home/kickret #{{:db/id 15
                    :stats/player {}
                    :kickret/returns 2
                    :kickret/tds 0
                    :kickret/long 57
                    :kickret/avg 38
                    :kickret/longtd 0}}
   
   ;; May add team stats later like time-of-possession, etc.

   ;; Home attributes are repeated for :visitor/*
   
   }
  )