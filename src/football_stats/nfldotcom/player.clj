(ns football-stats.nfldotcom.player
  (:require [datomic.api :refer [q] :as d]
            [net.cgrand.enlive-html :as html]))

(defn fetch-url [url]
  (html/html-resource (java.net.URL. url)))

(defn get-player-profile
  "Loads the player profile page"
  [playerid]
  (fetch-url (format "http://www.nfl.com/players/profile?id=%s"
                     playerid)))

(defn get-player-id
  "Gets the Datomic entity id for the given player NFL id"
  [db nflid]
  (ffirst
   (q '[:find ?p :in $ ?pid :where [?p :player/nflid ?pid]]
      db nflid)))

(defn get-player-by-nflid
  "Gets the player entity for the given NFL id"
  [db nflid]
  (d/entity db (get-player-id db nflid)))

(defn get-players
  "Gets a lazy seq of all the players"
  [db]
  (map #(d/entity db (first %))
       (q '[:find ?p :where [?p :player/nflid]] db)))

(defn needs-enrichment?
  "Does this player need enrichment? (position, etc.)"
  [player]
  (not (:player/position player)))

;; scratch...
(comment

  (def db (d/db (:conn user/system)))
  (get-player-by-nflid db "00-0028986")

  (-> (filter needs-enrichment? (get-players db))
      second
      :player/name)

  (html/select (get-player-profile "00-0028986") #{[:meta#playerId] [:meta#playerName] [:span.player-number]})


  )
