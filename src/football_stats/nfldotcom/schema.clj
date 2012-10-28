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
   [
    ;; Game
    {:db/id #db/id[:db.part/db]
     :db/ident :game/gameid
     :db/valueType :db.type/string
     :db/cardinality :db.cardinality/one
     :db/unique :db.unique/identity
     :db/doc "The unique nfl.com game ID (upsertable)."
     :db.install/_attribute :db.part/db}

    {:db/id #db/id[:db.part/db]
     :db/ident :game/nflraw
     :db/valueType :db.type/string
     :db/cardinality :db.cardinality/one
     :db/doc "The raw stats data from nfl.com"
     :db.install/_attribute :db.part/db}

    {:db/id #db/id[:db.part/db]
     :db/ident :game/home
     :db/valueType :db.type/ref
     :db/cardinality :db.cardinality/one
     :db/doc "A Game's home team information."
     :db.install/_attribute :db.part/db}

    ;; Game team
    {:db/id #db/id[:db.part/db]
     :db/ident :game.team/info
     :db/valueType :db.type/ref
     :db/cardinality :db.cardinality/one
     :db/doc "The team's information."
     :db.install/_attribute :db.part/db}

    {:db/id #db/id[:db.part/db]
     :db/ident :game.team/to
     :db/valueType :db.type/long
     :db/cardinality :db.cardinality/one
     :db/doc "The team's time-outs remaining."
     :db.install/_attribute :db.part/db}

    {:db/id #db/id[:db.part/db]
     :db/ident :game.team/score
     :db/valueType :db.type/ref
     :db/cardinality :db.cardinality/one
     :db/doc "The team's scoring by quarter."
     :db.install/_attribute :db.part/db}

    {:db/id #db/id[:db.part/db]
     :db/ident :game.team/stats
     :db/valueType :db.type/ref
     :db/cardinality :db.cardinality/one
     :db/doc "The home team stats."
     :db.install/_attribute :db.part/db}

    ;; Stats
    {:db/id #db/id[:db.part/db]
     :db/ident :stats/rushing
     :db/valueType :db.type/ref
     :db/cardinality :db.cardinality/many
     :db/doc "The team's rushing stats"
     :db.install/_attribute :db.part/db}

    ;; Rushing
    {:db/id #db/id[:db.part/db]
     :db/ident :rushing/player
     :db/valueType :db.type/ref
     :db/cardinality :db.cardinality/one
     :db/doc "The player that these rushing stats belong to."
     :db.install/_attribute :db.part/db}
    
    {:db/id #db/id[:db.part/db]
     :db/ident :rushing/yards
     :db/valueType :db.type/long
     :db/cardinality :db.cardinality/one
     :db/doc "Rushing yards"
     :db.install/_attribute :db.part/db}

    {:db/id #db/id[:db.part/db]
     :db/ident :rushing/attempts
     :db/valueType :db.type/long
     :db/cardinality :db.cardinality/one
     :db/doc "Rushing attempts"
     :db.install/_attribute :db.part/db}
    
    {:db/id #db/id[:db.part/db]
     :db/ident :rushing/tds
     :db/valueType :db.type/long
     :db/cardinality :db.cardinality/one
     :db/doc "Rushing TDs"
     :db.install/_attribute :db.part/db}

    {:db/id #db/id[:db.part/db]
     :db/ident :rushing/long
     :db/valueType :db.type/long
     :db/cardinality :db.cardinality/one
     :db/doc "Longest rush"
     :db.install/_attribute :db.part/db}

    {:db/id #db/id[:db.part/db]
     :db/ident :rushing/longtd
     :db/valueType :db.type/long
     :db/cardinality :db.cardinality/one
     :db/doc "Longest rushing TD"
     :db.install/_attribute :db.part/db}

    {:db/id #db/id[:db.part/db]
     :db/ident :rushing/twopt-attempts
     :db/valueType :db.type/long
     :db/cardinality :db.cardinality/one
     :db/doc "Rushing 2-pt attempts"
     :db.install/_attribute :db.part/db}

    {:db/id #db/id[:db.part/db]
     :db/ident :rushing/twopt-made
     :db/valueType :db.type/long
     :db/cardinality :db.cardinality/one
     :db/doc "Rushing 2-pt scores"
     :db.install/_attribute :db.part/db}

    
    ;; Scoring
    {:db/id #db/id[:db.part/db]
     :db/ident :score/q1
     :db/valueType :db.type/long
     :db/cardinality :db.cardinality/one
     :db/doc "The team's 1st quarter score."
     :db.install/_attribute :db.part/db}

    {:db/id #db/id[:db.part/db]
     :db/ident :score/q2
     :db/valueType :db.type/long
     :db/cardinality :db.cardinality/one
     :db/doc "The team's 2nd quarter score."
     :db.install/_attribute :db.part/db}

    {:db/id #db/id[:db.part/db]
     :db/ident :score/q3
     :db/valueType :db.type/long
     :db/cardinality :db.cardinality/one
     :db/doc "The team's 3rd quarter score."
     :db.install/_attribute :db.part/db}

    {:db/id #db/id[:db.part/db]
     :db/ident :score/q4
     :db/valueType :db.type/long
     :db/cardinality :db.cardinality/one
     :db/doc "The team's 4th quarter score."
     :db.install/_attribute :db.part/db}

    {:db/id #db/id[:db.part/db]
     :db/ident :score/ot
     :db/valueType :db.type/long
     :db/cardinality :db.cardinality/one
     :db/doc "The team's overtime score."
     :db.install/_attribute :db.part/db}

    {:db/id #db/id[:db.part/db]
     :db/ident :score/final
     :db/valueType :db.type/long
     :db/cardinality :db.cardinality/one
     :db/doc "The team's final score."
     :db.install/_attribute :db.part/db}

    ;; Player
    {:db/id #db/id[:db.part/db]
     :db/ident :player/nflid
     :db/valueType :db.type/string
     :db/cardinality :db.cardinality/one
     :db/unique :db.unique/identity
     :db/doc "The player's nfl.com id"
     :db.install/_attribute :db.part/db}
    
    {:db/id #db/id[:db.part/db]
     :db/ident :player/name
     :db/valueType :db.type/string
     :db/cardinality :db.cardinality/one
     :db/doc "The player's name"
     :db.install/_attribute :db.part/db}

    {:db/id #db/id[:db.part/db]
     :db/ident :player/position
     :db/valueType :db.type/string
     :db/cardinality :db.cardinality/one
     :db/doc "The player's position"
     :db.install/_attribute :db.part/db}

    {:db/id #db/id[:db.part/db]
     :db/ident :player/current-team
     :db/valueType :db.type/ref
     :db/cardinality :db.cardinality/one
     :db/doc "The player's current team"
     :db.install/_attribute :db.part/db}

    ;; Team
    {:db/id #db/id[:db.part/db]
     :db/ident :team/abbr
     :db/valueType :db.type/string
     :db/cardinality :db.cardinality/one
     :db/unique :db.unique/identity
     :db/doc "The teams abbreviation"
     :db.install/_attribute :db.part/db}

    {:db/id #db/id[:db.part/db]
     :db/ident :team/name
     :db/valueType :db.type/string
     :db/cardinality :db.cardinality/one
     :db/doc "The team's name (location)"
     :db.install/_attribute :db.part/db}
    
    {:db/id #db/id[:db.part/db]
     :db/ident :team/mascot
     :db/valueType :db.type/string
     :db/cardinality :db.cardinality/one
     :db/doc "The team's mascot"
     :db.install/_attribute :db.part/db}]))

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
  (create-metadata-txs))