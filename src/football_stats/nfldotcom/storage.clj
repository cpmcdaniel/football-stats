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

(defn get-player-id [db nflid]
  (first
   (first
    (q '[:find ?p :in $ ?pid :where [?p :player/nflid ?pid]]
       db nflid))))


(defn create-score-tx
  [team score-id]
  {:db/id score-id
   :score/q1 (-> team :score :1)
   :score/q2 (-> team :score :2)
   :score/q3 (-> team :score :3)
   :score/q4 (-> team :score :4)
   :score/ot (-> team :score :5)
   :score/final (-> team :score :T)})

(defn- prepare-stats [db stat-line]
  {:player-id (get-player-id db (name (first stat-line)))
   :stats (second stat-line)
   :stats-id (d/tempid :db.part/user)})

(defn- create-passing-txs
  [keyword-fn db db-gameid stat-line]
  (let [{:keys [player-id stats stats-id]}
        (prepare-stats db stat-line)]
    [{:db/id stats-id
      :stats/player player-id
      :passing/attempts (:att stats)
      :passing/complete (:cmp stats)
      :passing/yards (:yds stats)
      :passing/tds (:tds stats)
      :passing/ints (:ints stats)
      :passing/twopta (:twopta stats)
      :passing/twoptm (:twoptm stats)}
     [:db/add db-gameid (keyword-fn "passing") stats-id]]))

(defn- create-rushing-txs
  [keyword-fn db db-gameid stat-line]
  (let [{:keys [player-id stats stats-id]}
        (prepare-stats db stat-line)]
    [{:db/id stats-id
      :stats/player player-id
      :rushing/yards (:yds stats)
      :rushing/attempts (:att stats)
      :rushing/tds (:tds stats)
      :rushing/long (:lng stats)
      :rushing/longtd (:lngtd stats)
      :rushing/twopta (:twopta stats)
      :rushing/twoptm (:twoptm stats)}
     [:db/add db-gameid (keyword-fn "rushing") stats-id]]))

(defn- create-receiving-txs
  [keyword-fn db db-gameid stat-line]
  (let [{:keys [player-id stats stats-id]}
        (prepare-stats db stat-line)]
    [{:db/id stats-id
      :stats/player player-id
      :receiving/receptions (:rec stats)
      :receiving/yards (:yds stats)
      :receiving/tds (:tds stats)
      :receiving/long (:lng stats)
      :receiving/longtd (:lngtd stats)
      :receiving/twopta (:twopta stats)
      :receiving/twoptm (:twoptm stats)}
     [:db/add db-gameid (keyword-fn "receiving") stats-id]]))

(defn- create-kicking-txs
  [keyword-fn db db-gameid stat-line]
  (let [{:keys [player-id stats stats-id]}
        (prepare-stats db stat-line)]
    [{:db/id stats-id
      :stats/player player-id
      :kicking/fg-attempts (:fga stats)
      :kicking/fg-made (:fgm stats)
      :kicking/xp-attempts (:xpa stats)
      :kicking/xp-made (:xpmade stats)}
     [:db/add db-gameid (keyword-fn "kicking") stats-id]]))

(defn- create-puntret-txs
  [keyword-fn db db-gameid stat-line]
  (let [{:keys [player-id stats stats-id]}
        (prepare-stats db stat-line)]
    [{:db/id stats-id
      :stats/player player-id
      :puntret/returns (:ret stats)
      :puntret/tds (:tds stats)
      :puntret/long (:lng stats)
      :puntret/avg (:avg stats)
      :puntret/longtd (:lngtd stats)}
     [:db/add db-gameid (keyword-fn "puntret") stats-id]]))

(defn- create-kickret-txs
  [keyword-fn db db-gameid stat-line]
  (let [{:keys [player-id stats stats-id]}
        (prepare-stats db stat-line)]
    [{:db/id stats-id
      :stats/player player-id
      :kickret/returns (:ret stats)
      :kickret/tds (:tds stats)
      :kickret/long (:lng stats)
      :kickret/avg (:avg stats)
      :kickret/longtd (:lngtd stats)}
     [:db/add db-gameid (keyword-fn "kickret") stats-id]]))

(defn- create-defense-txs
  [keyword-fn db db-gameid stat-line]
  (let [{:keys [player-id stats stats-id]}
        (prepare-stats db stat-line)]
    [{:db/id stats-id
      :stats/player player-id
      :defense/sacks (double (:sk stats))
      :defense/ints (:int stats)
      :defense/tackles (:tkl stats)
      :defense/assists (:ast stats)
      :defense/fumbles-forced (:ffum stats)}
     [:db/add db-gameid (keyword-fn "defense") stats-id]]))

(defn- create-fumbles-txs
  [keyword-fn db db-gameid stat-line]
  (let [{:keys [player-id stats stats-id]}
        (prepare-stats db stat-line)]
    [{:db/id stats-id
      :stats/player player-id
      :fumbles/lost (:lost stats)
      :fumbles/recovered (:rcv stats)
      :fumbles/total (:tot stats)
      :fumbles/trcv (:trcv stats)}
     [:db/add db-gameid (keyword-fn "fumbles") stats-id]]))


(defn- create-stats-txs [stat-fn stats]
  (reduce concat (map stat-fn stats)))

(defn- create-game-team-txs
  [keyword-fn db team-data db-gameid]
  (let [scoreid (d/tempid :db.part/user)]
    (concat
     [[:db/add db-gameid (keyword-fn "team") (get-team-id db (:abbr team-data))]
      (create-score-tx team-data scoreid)
      [:db/add db-gameid (keyword-fn "score") scoreid]]
     (create-stats-txs
      (partial create-rushing-txs keyword-fn db db-gameid)
      (-> team-data :stats :rushing))
     (create-stats-txs
      (partial create-passing-txs keyword-fn db db-gameid)
      (-> team-data :stats :passing))
     (create-stats-txs
      (partial create-receiving-txs keyword-fn db db-gameid)
      (-> team-data :stats :receiving))
     (create-stats-txs
       (partial create-kicking-txs keyword-fn db db-gameid)
       (-> team-data :stats :kicking))
     (create-stats-txs
       (partial create-puntret-txs keyword-fn db db-gameid)
       (-> team-data :stats :puntret))
     (create-stats-txs
       (partial create-kickret-txs keyword-fn db db-gameid)
       (-> team-data :stats :kickret))
     (create-stats-txs
       (partial create-defense-txs keyword-fn db db-gameid)
       (-> team-data :stats :defense))
     (create-stats-txs
       (partial create-fumbles-txs keyword-fn db db-gameid)
       (-> team-data :stats :fumbles)))))

(def create-home-team-txs
  (partial create-game-team-txs #(keyword (str "home/" %))))

(def create-away-team-txs
  (partial create-game-team-txs #(keyword (str "away/" %))))

(defn create-game-txs
  [db nflgame]
  (let [db-gameid (d/tempid :db.part/user)]
     (cons
      {:db/id db-gameid
       :game/gameid (name (get-gameid nflgame))
       :game/nflraw (prn-str nflgame)
       ;:game/season (:season nflgame)
       ;:game/week (:week nflgame)
       :game/date (game-date nflgame)}
      (concat 
       (create-home-team-txs db (get-home-team nflgame) db-gameid)
       (create-away-team-txs db (get-away-team nflgame) db-gameid)))))

(defn create-player-txs
  [team-id players]
  (for [[player-id player-name] players]
    [:create-player (name player-id) player-name team-id]))

(defn store-players
  [conn nflgame]
  (let [db (d/db conn)
        home-id (get-team-id db (-> nflgame get-home-team :abbr))
        away-id (get-team-id db (-> nflgame get-away-team :abbr))]
    @(d/transact
     conn
     (concat
      (create-player-txs home-id (get-home-players nflgame))
      (create-player-txs away-id (get-away-players nflgame))))))

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



