(ns football-stats.nfldotcom.storage
  (:use [datomic.api :only [q] :as d]
        football-stats.nfldotcom.api))

(defn get-db-ids [txs]
  (hash-set
   (filter #(not (nil? %))
    (map
     (fn [tx]
       (cond (map? tx) (:db/id tx)
             (vector? tx) (tx 1)
             :default nil)) txs))))

(defn get-team-id [db abbr]
  (first
   (first
    (q '[:find ?t :in $ ?abbr :where [?t :team/abbr ?abbr]] db abbr))))

(comment ;; TODO refactor this
  (defn create-rushing-txs
    [rushing-stats]
    (for [[playerid stats] rushing-stats]
      {:db/id #db/id[:db.part/user]
       :rushing/yards (:yds stats)
       :rushing/attempts (:att stats)
       :rushing/tds (:tds stats)
       :rushing/long (:lng stats)
       :rushing/longtd (:lngtd stats)
       :rushing/twopt-attempts (:twopta stats)
       :rushing/twopt-made (:twoptm stats)})))

(defn create-score-tx
  [home-team score-id]
  {:db/id score-id
   :score/q1 (-> home-team :score :1)
   :score/q2 (-> home-team :score :2)
   :score/q3 (-> home-team :score :3)
   :score/q4 (-> home-team :score :4)
   :score/ot (-> home-team :score :5)
   :score/final (-> home-team :score :T)})

(defn create-game-team-txs
  [db in-team game-team-id]
  (let [scoreid (d/tempid :db.part/user)]
   [{:db/id game-team-id
     :game.team/info (get-team-id db (:abbr in-team))
     :game.team/to (:to in-team)
     :game.team/score scoreid}
    (create-score-tx in-team scoreid)]))

(defn create-game-txs
  [db nflgame]
  (let [homeid (d/tempid :db.part/user)]
     (cons
      {:db/id #db/id[:db.part/user -1]
       :game/gameid (name (get-gameid nflgame))
       :game/nflraw (prn-str nflgame)
       :game/home homeid}
      (create-game-team-txs db (get-home-team nflgame) homeid))))

(defn create-player-txs
  [team-id players]
  (for [[player-id player-name] players]
    [:create-player (name player-id) player-name team-id]))

(defn store-players
  [conn nflgame]
  (let [db (d/db conn)
        home-id (get-team-id db (-> nflgame get-home-team :abbr))
        visitor-id (get-team-id db (-> nflgame get-visitor-team :abbr))]
    ;; TODO is this a future?
    @(d/transact
     conn
     (concat
      (create-player-txs home-id (get-home-players nflgame))
      (create-player-txs visitor-id (get-visitor-players nflgame))))))

(defn store-game [conn nflgame]
  ;; First, create new players, if needed.
  ;; The subsequent tx can then lookup the players.
  (store-players conn nflgame)
  (d/transact conn (create-game-txs (d/db conn) nflgame)))

(defn find-player-by-nflid
  "Finds the player entity for the specified nfl.com player id.
   Returns nil if the given player id is not found."
  [db playerid]
  (first
   (first
    (q '[:find ?p :where
         [?p :player/nflid playerid]]))))



(comment
  ;; play area
  (def txs
   (let [nflgame (read-string (slurp "test/data/2011090800"))
         home-team (-> nflgame :2011090800 :home)]

     (create-game-txs nflgame)))

  (-> txs rest)

  )