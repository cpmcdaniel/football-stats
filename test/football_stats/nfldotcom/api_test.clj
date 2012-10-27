(ns football-stats.nfldotcom.api-test
  (:use clojure.test
        football-stats.nfldotcom.api))

(def nflgame (read-string (slurp "test/data/2011090800")))

(deftest test-replace-empty-keywords
  (let [json {:x "a" :b {:c "1" (keyword "") "0"}}
        clean (replace-empty-keywords json)]
    (is (re-find #":\s" (prn-str json)))
    (is (re-find #":_" (prn-str clean)))
    (is (not (re-find #":\s" (prn-str clean))))))

(deftest test-game-center-id
  (is
   (= "2011090800"
      (link->game-center-id 
       "http://www.nfl.com/gamecenter/2011090800/2011/REG1/saints@packers"))))

(deftest test-get-save-directory-default
  (is (= "./stats" (get-save-directory))))

(deftest test-get-gameid
  (is (= :2011090800 (get-gameid nflgame))))

(deftest test-get-home-team
  (let [gamestats ((get-gameid nflgame) nflgame)]
    (is (= "GB" (:abbr (get-home-team gamestats))))))

(deftest test-get-team-info
  (let [team-info (get-team-info "GB")]
    (is (= "GB" (:abbr team-info)))
    (is (= "Green Bay" (:city team-info)))
    (is (= "Packers" (:nickname team-info)))))