(ns football-stats.nfldotcom.storage
  (:use [datomic.api :only [q db] :as d]
        football-stats.nfldotcom.api))

(defn- get-db-ids [txs]
  (hash-set
   (filter #(not (nil? %))
    (map
     (fn [tx]
       (cond (map? tx) (:db/id tx)
             (vector? tx) (tx 1)
             :default nil)) txs))))

(defn- create-rushing-txs
  [rushing-stats]
  (for [[playerid stats] rushing-stats]
    {:db/id #db/id[:db.part/user]
     :rushing/yards (:yds stats)
     :rushing/attempts (:att stats)
     :rushing/tds (:tds stats)
     :rushing/long (:lng stats)
     :rushing/longtd (:lngtd stats)
     :rushing/twopt-attempts (:twopta stats)
     :rushing/twopt-made (:twoptm stats)}))

(defn- create-stats-txs
  [home-team stats-id]
  (let [rushing-txs (create-rushing-txs
                     (:rushing (:stats home-team)))
        ;; etc...
        ]
    (cons {:db/id stats-id
           :stats/rushing (get-db-ids rushing-txs)
           ;; :stats/passing passing-stats-id
           ;; etc...
           }
          rushing-txs)))

(defn- create-score-tx
  [home-team score-id]
  (cons 
   {:db/id score-id
    :score/q1 (-> home-team :score :1)
    :score/q2 (-> home-team :score :2)
    :score/q3 (-> home-team :score :3)
    :score/q4 (-> home-team :score :4)
    :score/ot (-> home-team :score :5)
    :score/final (-> home-team :score :T)}))

(defn- create-home-txs
  [home-team]
  (conj
   [{:db/id #db/id[:db.part/user -2]
     :game.team/abbr (:abbr home-team)
     :game.team/to (:to home-team)
     :game.team/score #db/id[:db.part/user -3]
     :game.team/stats #db/id[:db.part/user -4]}
    (create-score-tx home-team #db/id[:db.part/user -3])]
   (create-stats-txs home-team #db/id[:db.part/user -4])))

(defn store-game [nflgame conn]
  (d/transact
   conn
   (let [gameid (get-gameid nflgame)
         gamestats (gameid nflgame)]
     (cons
      {:db/id #db/id[:db.part/user -1]
       :game/gameid (name gameid)
       :game/nflraw (prn-str nflgame)
       :game/home #db/id[:db.part/user -2]}
      (create-home-txs (get-home-team gamestats))))))


(defn find-player-by-nflid
  "Finds the player entity for the specified nfl.com player id.
   Returns nil if the given player id is not found."
  [db playerid]
  (first
   (first
    (q '[:find ?p :where
         [?p :player/nflid playerid]]))))

;; Data/Tx functions
;; if the parameter lists change, you will also need to change them
;; in schema.clj
(defn link-player
  "Link to the player, if he exists. Otherwise, create the player
   record and link to the new player."
  [db
   entity-to-link
   attr-for-link
   nfl-playerid
   player-name
   team]
  (let [playerid (find-player-by-nflid db)]
    (if (nil? playerid)
      ;; create the player record
      (let [new-playerid (d/tempid)]
        [[:create-player new-playerid nfl-playerid player-name team]
         [:db/add entity-to-link attr-for-link new-playerid]])
      [[:db/add entity-to-link attr-for-link playerid]])))

(defn create-player
  [db e nflid name team]
  [{:db/id e
    :player/nflid nflid
    :player/name name}])
