(ns football-stats.nfldotcom.api
  (:require [clj-webdriver.taxi :as taxi]
            [clj-http.client :as http]
            [clojure.walk :as walk]
            [lamina.core :as l]
            [clojure.tools.logging :refer [debug warn]])
  (:use [clj-http.util :only [url-encode]]
        [clojure.java.io :only [file make-parents resource]]
        [cheshire.core :only [parse-string]]
        retry.core))

(defrecord Game [season week gameid stats])

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
  [{:keys [gameid] :as game}]
  (loop [sleep 1]
    (when (< 1 sleep) (debug "Sleeping for " sleep " second(s)."))
    (Thread/sleep (* 1000 sleep))
    (let [response
          (http/get
           (format
            "http://www.nfl.com/liveupdate/game-center/%s/%s_gtd.json"
            gameid gameid) {:accept :json :as :json :throw-exceptions false})]
      (if (http/success? response)
        (assoc game :stats (replace-empty-keywords (:body response)))
        (if (or (http/missing? response) (< 4 sleep))
          (do (warn "Giving up on game" gameid)
              game)
          (recur (inc sleep)))))))


(defn- goto-weekly-stats
  "Goes to the weekly stats page for the specified Season and Week.
    The Week param should be one of: PRE[1-4], REG[1-17], POST[18-20,22], PRO21"
  [{:keys [season week web-driver]}]
  ;; TODO Check preconditions for both season and week
  (taxi/to web-driver (str "http://www.nfl.com/scores/" (url-encode (str season)) "/" (url-encode (str week)))))

(defn- week-from-link
  "Extracts the week from the anchor tag."
  [web-driver anchor]
  (let [link (taxi/attribute web-driver anchor :href)]
    (second (re-find #"/(\w+)$" link))))

(defn- find-weeks
  "Finds the links to each weekly stats page."
  [web-driver]
  (taxi/find-elements web-driver {:css "a.week-item"}))

(defn game-center-season-weeks
  "Gets all week names for the specified season."
  [{:keys [web-driver season] :as context}]
  (goto-weekly-stats (assoc context :week "REG1"))
  (doall
   ;; The web-driver does not work well with lazy seqs.
   (map (partial week-from-link web-driver) (find-weeks web-driver))))

(defn- find-game-centers [web-driver]
  (taxi/find-elements web-driver {:css "a.game-center-link"}))

(defn scrape-game-center-links
  "Gets all the Game Center link elements on the page as seq"
  [{:keys [season week web-driver] :as context}]
  (goto-weekly-stats context)
  (doall ;; the web-driver doesn't play nice with lazy seqs
   (map #(taxi/attribute web-driver % :href) (find-game-centers web-driver))))

(defn link->game-center-id
  "Extract game id from the game center link"
  [link]
  (second (re-find #"gamecenter/(\d+)" link)))

(defn links->game-center-ids
  "Extract game ids from the game center links"
  [links]
  (map link->game-center-id links))

(defn game-center-games
  "Gets all game records for the specified seasons. Returns
    a lazy sequence of Games."
  [web-driver seasons]
  (for [season seasons
        week (game-center-season-weeks {:season season :web-driver web-driver})
        gameid (links->game-center-ids
                (scrape-game-center-links {:season season :week week
                                           :web-driver web-driver}))]
    (Game. season week gameid {})))

(defn save-games
  "Takes a sequence of games. Requests the stats and saves
    them for each game."
  [games storage-channel]
  (doseq [game games]
    (l/enqueue storage-channel (get-game-stats game))))

(defn save-seasons-from-web
  "Saves stats for all games in the specified seasons."
  [system seasons]
  (let [web-driver (taxi/new-driver {:browser :firefox})]
    (doseq [season seasons]
      (l/enqueue (:season-channel system)
                 {:season season :web-driver web-driver}))
    (taxi/close web-driver)))

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

(defn create-week-channel
  "Receives a message with :season and :week keys and uses web-driver to scrape the game center ids. It then constructs Game records and sends them to the storage channel"
  [storage-channel]
  (l/sink->>
   (l/filter* #(not (.startsWith (:week %) "PRO"))) ;; Ignore pro-bowl
   (fn [{:keys [season week web-driver] :as message}]
     (doseq [gameid (links->game-center-ids
                     (scrape-game-center-links message))]
       (l/enqueue storage-channel
                  (get-game-stats (Game. season week gameid {})))))))

(defn create-season-channel
  "Receives a message with a specified season, requests all weeks for that season, then sends a request for each week onto the week-channel."
  [week-channel]
  (l/sink (fn [{:keys [season web-driver] :as message}]
            (doseq [week (game-center-season-weeks message)]
              (l/enqueue week-channel (assoc message :week week))))))

(defn- download-seasons
  "Enqueues each season on the given channel with the given web-driver"
  [season-channel web-driver & seasons]
  (doseq [season seasons]
    (l/enqueue season-channel
               {:web-driver web-driver
                :season season})))

(defn create-driver-channels
  "Creates all web-driver channels."
  [storage-channel]
  (let [week-channel (create-week-channel storage-channel)
        season-channel (create-season-channel week-channel)]
    {:week-channel week-channel
     :season-channel season-channel
     :download-seasons (partial download-seasons season-channel)}))

(defn close-driver-channels
  [{:keys [week-channel season-channel]}]
  (l/close week-channel)
  (l/close season-channel))
