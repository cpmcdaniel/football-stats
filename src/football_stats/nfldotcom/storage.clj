(ns football-stats.nfldotcom.storage
  (:require [datomic.api :refer [q] :as d]
            [football-stats.nfldotcom.api :refer :all]
            [lamina.core :as l]
            [clojure.java.io :as io]))

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
  [db {:keys [season week gameid stats] :as game}]
  (let [db-gameid (d/tempid :db.part/user)]
     (cons
      {:db/id db-gameid
       :game/gameid gameid
       :game/nflraw (prn-str stats)
       :game/season season
       :game/week week
       :game/date (game-date stats)}
      (concat 
       (create-home-team-txs db (get-home-team stats) db-gameid)
       (create-away-team-txs db (get-away-team stats) db-gameid)))))

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

(defn store-game [conn {:keys [stats] :as game}]
  ;; First, create new players, if needed.
  ;; The subsequent tx can then lookup the players.
  (store-players conn stats)
  (d/transact conn (create-game-txs (d/db conn) game)))

(defn find-player-by-nflid
  "Finds the player entity for the specified nfl.com player id.
   Returns nil if the given player id is not found."
  [db playerid]
  (first
   (first
    (q '[:find ?p :where
         [?p :player/nflid playerid]]))))

(defn get-save-directory
  "Gets the value of the stats.dir system property. Defaults
    to './stats' if the property isn't set."
  []
  (System/getProperty "stats.dir" "./stats"))

(defn game-directory
  "Gets the directory for the specified season and week.
    Creates the directory if it does not exist."
  [{:keys [season week gameid]}]
  (let [game-dir (io/file (get-save-directory) (str season) week)]
    (when-not (.exists game-dir) (io/make-parents game-dir gameid))
    game-dir))

(defn save-game-to-file
  "Takes a game record and saves it to a file."
  [{:keys [season week gameid stats] :as game}]
  (when (and season week gameid stats)
    (println (format "Saving game %d %s %s" season week gameid))
    (let [game-file (io/file (game-directory game) gameid)]
      (spit game-file (pr-str game)))))

(defn create-file-channel
  "Creates a lamina channel for storing a game to a file."
  [storage-channel]
  (let [file-channel (l/sink save-game-to-file)]
    (l/siphon (l/fork storage-channel) file-channel)
    file-channel))

(defn create-datomic-channel
  "Creates a lamina channel for storing a game in the database."
  [conn storage-channel]
  (let [datomic-channel
        (l/sink (fn [game]
                  (store-game conn game)))]
    (l/siphon (l/fork storage-channel) datomic-channel)
    datomic-channel))

(defn create-storage-channels
  "Creates the lamina channels for storage and returns them in a map."
  [conn]
  (let [storage-channel (l/channel)]
    {:storage-channel storage-channel
     :file-channel (create-file-channel storage-channel)
     :datomic-channel (create-datomic-channel conn storage-channel)}))

(defn close-storage-channels
  "Closes all storage channels"
  [{:keys [storage-channel file-channel datomic-channel]}]
  (l/close datomic-channel)
  (l/close file-channel)
  (l/close storage-channel))
