(ns football-stats.nfldotcom.api
  (:require [clj-webdriver.taxi :as taxi]
            [clj-http.client :as http]
            [clojure.walk :as walk])
  (:use [clj-http.util :only [url-encode]]
        [clojure.java.io :only [file make-parents resource]]
        [cheshire.core :only [parse-string]]
        retry.core))

(defn get-gameid [nflgameraw]
  (first (filter #(not (= :nextupdate %)) (keys nflgameraw))))

(defn to-game-stats [m]
  (if (:home m) m
      ((get-gameid m) m)))

(defn get-home-team [game]
  (:home (to-game-stats game)))

(defn get-away-team [game]
  (:away (to-game-stats game)))

(defn replace-empty-keywords
  [json]
  (walk/postwalk-replace {(keyword "") :_} json))

(defn get-game-stats
  "Gets game stats as a Clojure map (converted directly from JSON)"
  [^String gameid]
  (loop [sleep 1]
    (when (< 1 sleep) (println (str "Sleeping for " sleep " second(s).")))
    (Thread/sleep (* 1000 sleep))
    (let [response
          (http/get
           (format
            "http://www.nfl.com/liveupdate/game-center/%s/%s_gtd.json"
            gameid gameid) {:accept :json :as :json :throw-exceptions false})]
      (if (http/success? response)
        (replace-empty-keywords (:body response))
        (if (< 4 sleep)
          (println (str "Giving up on game " gameid))
          (recur (inc sleep)))))))


(defn- goto-weekly-stats
  "Goes to the weekly stats page for the specified Season and Week.
    The Week param should be one of: PRE[1-4], REG[1-17], POST[18-20,22], PRO21"
  [season week]
  ;; TODO Check preconditions for both season and week
  (taxi/to (str "http://www.nfl.com/scores/" (url-encode (str season)) "/" (url-encode (str week)))))

(defn- week-from-link
  "Extracts the week from the anchor tag."
  [anchor]
  (let [link (taxi/attribute anchor :href)]
    (second (re-find #"/(\w+)$" link))))

(defn- find-weeks
  "Finds the links to each weekly stats page."
  []
  (taxi/find-elements {:css "a.week-item"}))

(defn game-center-season-weeks
  "Gets all week names for the specified season."
  [season]
  (goto-weekly-stats season "REG1")
  (doall
   ;; The web-driver does not work well with lazy seqs.
   (map week-from-link (find-weeks))))

(defn- find-game-centers []
  (taxi/find-elements {:css "a.game-center-link"}))

(defn scrape-game-center-links
  "Gets all the Game Center link elements on the page as seq"
  [season week]
  (goto-weekly-stats season week)
  (doall ;; the web-driver doesn't play nice with lazy seqs
   (map #(taxi/attribute % :href) (find-game-centers))))

(defn link->game-center-id
  "Extract game id from the game center link"
  [link]
  (second (re-find #"gamecenter/(\d+)" link)))

(defn links->game-center-ids
  "Extract game ids from the game center links"
  [links]
  (map link->game-center-id links))

(defn game-center-seasons
  "Gets all game center ids for the specified seasons. Returns
    a lazy sequence of vectors containing [season week gameid]."
  [seasons]
  (for [season seasons
        week (game-center-season-weeks season)
        gameid (links->game-center-ids
                (scrape-game-center-links season week))]
    [season week gameid]))

(defn get-save-directory
  "Gets the value of the stats.dir system property. Defaults
    to './stats' if the property isn't set."
  []
  (System/getProperty "stats.dir" "./stats"))

(defn game-directory
  "Gets the directory for the specified season and week.
    Creates the directory if it does not exist."
  [season week gameid]
  (let [game-dir (file (get-save-directory) (str season) week)]
    (when-not (.exists game-dir) (make-parents game-dir gameid))
    game-dir))

(defn save-game-stats
  "Takes a sequence of games. Requests the stats and saves
    them for each game."
  [games]
  (doseq [[season week gameid] games]
    (println (format "Saving game %d %s %s" season week gameid))
    (when-let [game-stats (get-game-stats gameid)]
      (let [game-file (file (game-directory season week gameid) gameid)]
        (spit game-file (pr-str game-stats))))))

(defn save-seasons
  "Saves stats to the file system for all games in the specified seasons."
  [seasons]
  (taxi/with-driver {:browser :firefox}
    (save-game-stats (game-center-seasons seasons))))

(defn driver []
  (taxi/set-driver! {:browser :firefox}))

(defn get-team-info
  "Gets the team information for the given team abbreviation."
  [abbr]
  ((keyword abbr)
   (parse-string
    (slurp
     (resource "football_stats/nfldotcom/team-data.json"))
    true)))

(defn get-players-from-stats
  [stat-lines]
  (into {} (for [[player-id stat-line] stat-lines]
             [player-id (:name stat-line)])))

(defn get-team-players
  [team-stats]
  (if (map? team-stats)
    (if (some #(re-find #"\d{2}-\d{7}" (name %)) (keys team-stats))
      (get-players-from-stats team-stats)
      (reduce merge (map get-team-players (vals team-stats))))
    nil))

(defn get-home-players [m]
  (-> m to-game-stats :home get-team-players))

(defn get-away-players [m]
  (-> m to-game-stats :away get-team-players))

(defn game-date [g]
  (if (map? g) (game-date (get-gameid g))
      (let [sdf (new java.text.SimpleDateFormat "yyyyMMdd")]
        (.parse sdf (.substring (name g) 0 8)))))

(comment
  (taxi/with-driver {:browser :firefox}
    (get-game-stats (first (game-center-ids (game-center-links 2012 "REG8")))))
  )