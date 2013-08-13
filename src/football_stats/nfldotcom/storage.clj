(ns football-stats.nfldotcom.storage
  (:require [datomic.api :refer [q] :as d]
            [clojure.tools.logging :refer [debugf info error]]
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
  (ffirst
   (q '[:find ?t :in $ ?abbr :where [?t :team/abbr ?abbr]] db abbr)))

(defn get-player-id [db nflid]
  (ffirst
   (q '[:find ?p :in $ ?pid :where [?p :player/nflid ?pid]]
      db nflid)))

(defn get-game [db nfl-gameid]
  (d/entity
   (ffirst
    (q '[:find ?g :in $ ?gid :where [?g :game/gameid ?gid]]
       db nfl-gameid))))

(defn create-score-tx
  [team score-id]
  (let [score (team "score")]
   {:db/id score-id
    :score/q1 (get score "1" 0)
    :score/q2 (get score "2" 0)
    :score/q3 (get score "3" 0)
    :score/q4 (get score "4" 0)
    :score/ot (get score "5" 0)
    :score/final (get score "T" 0)}))

(defn- prepare-stats [db stat-line]
  {:player-id (get-player-id db (first stat-line))
   :stats (second stat-line)
   :stats-id (d/tempid :db.part/user)})

(defn- create-passing-txs
  [keyword-fn db db-gameid stat-line]
  (let [{:keys [player-id stats stats-id]}
        (prepare-stats db stat-line)]
    [{:db/id stats-id
      :stats/player player-id
      :passing/attempts (get stats "att" 0)
      :passing/complete (get stats "cmp" 0)
      :passing/yards (get stats "yds" 0)
      :passing/tds (get stats "tds" 0)
      :passing/ints (get stats "ints" 0)
      :passing/twopta (get stats "twopta" 0)
      :passing/twoptm (get stats "twoptm" 0)}
     [:db/add db-gameid (keyword-fn "passing") stats-id]]))

(defn- create-rushing-txs
  [keyword-fn db db-gameid stat-line]
  (let [{:keys [player-id stats stats-id]}
        (prepare-stats db stat-line)]
    [{:db/id stats-id
      :stats/player player-id
      :rushing/yards (get stats "yds" 0)
      :rushing/attempts (get stats "att" 0)
      :rushing/tds (get stats "tds" 0)
      :rushing/long (get stats "lng" 0)
      :rushing/longtd (get stats "lngtd" 0)
      :rushing/twopta (get stats "twopta" 0)
      :rushing/twoptm (get stats "twoptm" 0)}
     [:db/add db-gameid (keyword-fn "rushing") stats-id]]))

(defn- create-receiving-txs
  [keyword-fn db db-gameid stat-line]
  (let [{:keys [player-id stats stats-id]}
        (prepare-stats db stat-line)]
    [{:db/id stats-id
      :stats/player player-id
      :receiving/receptions (get stats "rec" 0)
      :receiving/yards (get stats "yds" 0)
      :receiving/tds (get stats "tds" 0)
      :receiving/long (get stats "lng" 0)
      :receiving/longtd (get stats "lngtd" 0)
      :receiving/twopta (get stats "twopta" 0)
      :receiving/twoptm (get stats "twoptm" 0)}
     [:db/add db-gameid (keyword-fn "receiving") stats-id]]))

(defn- create-kicking-txs
  [keyword-fn db db-gameid stat-line]
  (let [{:keys [player-id stats stats-id]}
        (prepare-stats db stat-line)]
    [{:db/id stats-id
      :stats/player player-id
      :kicking/fg-attempts (get stats "fga" 0)
      :kicking/fg-made (get stats "fgm" 0)
      :kicking/xp-attempts (get stats "xpa" 0)
      :kicking/xp-made (get stats "xpmade" 0)}
     [:db/add db-gameid (keyword-fn "kicking") stats-id]]))

(defn- create-puntret-txs
  [keyword-fn db db-gameid stat-line]
  (let [{:keys [player-id stats stats-id]}
        (prepare-stats db stat-line)]
    [{:db/id stats-id
      :stats/player player-id
      :puntret/returns (get stats "ret" 0)
      :puntret/tds (get stats "tds" 0)
      :puntret/long (get stats "lng" 0)
      :puntret/avg (get stats "avg" 0)
      :puntret/longtd (get stats "lngtd" 0)}
     [:db/add db-gameid (keyword-fn "puntret") stats-id]]))

(defn- create-kickret-txs
  [keyword-fn db db-gameid stat-line]
  (let [{:keys [player-id stats stats-id]}
        (prepare-stats db stat-line)]
    [{:db/id stats-id
      :stats/player player-id
      :kickret/returns (get stats "ret" 0)
      :kickret/tds (get stats "tds" 0)
      :kickret/long (get stats "lng" 0)
      :kickret/avg (get stats "avg" 0)
      :kickret/longtd (get stats "lngtd" 0)}
     [:db/add db-gameid (keyword-fn "kickret") stats-id]]))

(defn- create-defense-txs
  [keyword-fn db db-gameid stat-line]
  (let [{:keys [player-id stats stats-id]}
        (prepare-stats db stat-line)]
    [{:db/id stats-id
      :stats/player player-id
      :defense/sacks (double (get stats "sk" 0))
      :defense/ints (get stats "int" 0)
      :defense/tackles (get stats "tkl" 0)
      :defense/assists (get stats "ast" 0)
      :defense/fumbles-forced (get stats "ffum" 0)}
     [:db/add db-gameid (keyword-fn "defense") stats-id]]))

(defn- create-fumbles-txs
  [keyword-fn db db-gameid stat-line]
  (let [{:keys [player-id stats stats-id]}
        (prepare-stats db stat-line)]
    [{:db/id stats-id
      :stats/player player-id
      :fumbles/lost (get stats "lost" 0)
      :fumbles/recovered (get stats "rcv" 0)
      :fumbles/total (get stats "tot" 0)
      :fumbles/trcv (get stats "trcv" 0)}
     [:db/add db-gameid (keyword-fn "fumbles") stats-id]]))


(defn- create-stats-txs [stat-fn stats]
  (reduce concat (map stat-fn stats)))

(defn- create-game-team-txs
  [keyword-fn db team-data db-gameid]
  (let [scoreid (d/tempid :db.part/user)
        team-stats (team-data "stats")]
    (concat
     [[:db/add db-gameid (keyword-fn "team") (get-team-id db (team-data "abbr"))]
      (create-score-tx team-data scoreid)
      [:db/add db-gameid (keyword-fn "score") scoreid]]
     (create-stats-txs
      (partial create-rushing-txs keyword-fn db db-gameid)
      (team-stats "rushing"))
     (create-stats-txs
      (partial create-passing-txs keyword-fn db db-gameid)
      (team-stats "passing"))
     (create-stats-txs
      (partial create-receiving-txs keyword-fn db db-gameid)
      (team-stats "receiving"))
     (create-stats-txs
      (partial create-kicking-txs keyword-fn db db-gameid)
      (team-stats "kicking"))
     (create-stats-txs
      (partial create-puntret-txs keyword-fn db db-gameid)
      (team-stats "puntret"))
     (create-stats-txs
      (partial create-kickret-txs keyword-fn db db-gameid)
      (team-stats "kickret"))
     (create-stats-txs
      (partial create-defense-txs keyword-fn db db-gameid)
      (team-stats "defense"))
     (create-stats-txs
      (partial create-fumbles-txs keyword-fn db db-gameid)
      (team-stats "fumbles")))))

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
        home-id (get-team-id db (-> nflgame get-home-team (get "abbr")))
        away-id (get-team-id db (-> nflgame get-away-team (get "abbr")))]
    (d/transact
     conn
     (concat
      (create-player-txs home-id (get-home-players nflgame))
      (create-player-txs away-id (get-away-players nflgame))))))

(defn store-game [conn {:keys [stats season week gameid] :as game}]
  ;; First, create new players, if needed.
  ;; The subsequent tx can then lookup the players.
  (debugf "Saving game %d %s %s in db" season week gameid)
  @(store-players conn stats)
  (d/transact conn (create-game-txs (d/db conn) game)))


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
    (debugf "Saving game %d %s %s to file" season week gameid)
    (let [game-file (io/file (game-directory game) gameid)]
      (spit game-file (pr-str game)))))

(defn create-file-channel
  "Creates a lamina channel for storing a game to a file."
  [storage-channel]
  (let [file-channel (l/sink save-game-to-file)]
    (when storage-channel
      (l/siphon (l/fork storage-channel) file-channel))
    file-channel))

(defn create-datomic-channel
  "Creates a lamina channel for storing a game in the database."
  [conn storage-channel]
  (let [datomic-channel
        (l/sink (fn [game]
                  @(store-game conn game)))]
    (when storage-channel
      (l/siphon (l/fork storage-channel) datomic-channel))
    datomic-channel))

(defn create-storage-channels
  "Creates the lamina channels for storage and returns them in a map."
  [conn]
  (let [storage-channel (->>
                         (l/channel)
                         (l/filter* #(not-empty (:stats %))))]
    {:storage-channel storage-channel
     :file-channel (create-file-channel storage-channel)
     :datomic-channel (when conn (create-datomic-channel conn storage-channel))}))

(defn close-storage-channels
  "Closes all storage channels"
  [{:keys [storage-channel file-channel datomic-channel]}]
  (l/close datomic-channel)
  (l/close file-channel)
  (l/close storage-channel))


(defn file->game
  [^java.io.File file]
  (read-string (slurp file)))

(defn files->games
  []
  (->> (get-save-directory)
       io/file
       file-seq
       (filter #(.isFile %))
       sort
       (map file->game)))

(defn files->datomic
  [datomic-channel]
  (doseq [game (files->games)]
    (l/enqueue datomic-channel game)))
