(ns scratch
  (:require [football-stats.nfldotcom.storage :as storage]
            [clojure.java.io :as io]
            [clojure.pprint :refer [pprint]]
            [datomic.api :as d]
            [user :refer [system]]))


;; Bad file?
;; 2009 REG10 2009111505
(def bad-game
  (storage/file->game
   (io/file (storage/game-directory {:season 2009 :week "REG10" :gameid "2009111506"})
            "2009111506")))

(comment
  (def bad-game-txs (storage/create-game-txs (d/db (:conn system)) bad-game))

  (loop [txs bad-game-txs idx 0]
    (when-let [tx (first txs)]
      (do (println idx ":")
          (pprint tx)
          (d/transact (:conn system) [tx])
          (recur (rest txs) (inc idx))))))

(comment
  (use 'clj-webdriver.taxi)

  (set-driver! {:browser :firefox} "http://www.nfl.com/scores/2011/REG10")
  (to (attribute "a.game-center-link" :href))
  (click {:xpath "//a[@href='#analyze']"})
  (pprint (html {:xpath "//tr[@class='tbdy1']/td/a"}))
  (pprint (text {:xpath "//tr[@class='tbdy1']/td/a"}))
  (close))


