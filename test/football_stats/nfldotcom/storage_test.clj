(ns football-stats.nfldotcom.storage-test
  (:require [clojure.test :refer :all]
            [clojure.pprint :refer [pprint]]
            [football-stats.nfldotcom.storage :refer :all]
            [football-stats.nfldotcom.schema :refer [ensure-migrations]]
            [football-stats.nfldotcom.api :as api]
            [datomic.api :as d :refer [q db]])
  (:import [football_stats.nfldotcom.api Game]))

(def ^:dynamic conn)

(def datomic-test-uri "datomic:mem://test")

(def nflgame (Game.
              2011
              "REG3"
              "2011090800"
              (read-string (slurp "test/data/2011090800"))))

(defn datomic-fixture [t]
  ;; Setup
  (d/delete-database datomic-test-uri)
  (d/create-database datomic-test-uri)
  (binding [conn (d/connect datomic-test-uri)]
    (ensure-migrations conn)
    (t)) ;; execute test
  )

(use-fixtures :once datomic-fixture)

(defn find-test-game []
  (q '[:find ?g :where
       [?g :game/gameid "2011090800"]] (db conn)))

(defn get-test-game []
  (d/entity (db conn) (ffirst (find-test-game))))

(defn test-player-stats 
  [parent-key stat-keys]
  (let [results (q `[:find ?p :where [?x ~parent-key ?p]] (db conn))
        stat-line (d/entity (db conn) (ffirst results))]
    (is (< 0 (count results)))
    (is (-> stat-line :stats/player :player/name nil? not))
    (for [k stat-keys]
      (is (number? (k stat-line))))))


(deftest test-get-team-id
  (let [team (d/touch
              (d/entity
               (db conn) (get-team-id (db conn) "GB")))]
    (is (= "Packers" (:team/mascot team)))))

(deftest test-store-players
  @(store-players conn (:stats nflgame))
  (is (= "Saints" (ffirst (q '[:find ?t :where
                              [?p :player/name "D.Brees"]
                              [?p :player/current-team ?c]
                              [?c :team/mascot ?t]]
                            (db conn))))))

(deftest test-create-score-tx
  (let [tx (create-score-tx (api/get-home-team (:stats nflgame)) 1)]
    (is (:score/q1 tx))
    (is (:score/q2 tx))
    (is (:score/q3 tx))
    (is (:score/q4 tx))
    (is (:score/ot tx))
    (is (:score/final tx))))

(deftest test-store-game
  (testing "Store game"
    @(store-game conn nflgame))
  (testing "Find game"
    (is (= 1 (count (find-test-game)))))

  (testing "Home team"
    (let [home-team (:home/team (get-test-game))]
      (is (= "GB" (:team/abbr home-team)))))
  (testing "Home scoring"
    (let [scoring (:home/score (get-test-game))]
      (is (= 21 (:score/q1 scoring)))
      (is (= 7 (:score/q2 scoring)))
      (is (= 7 (:score/q3 scoring)))
      (is (= 7 (:score/q4 scoring)))
      (is (= 0 (:score/ot scoring)))
      (is (= 42 (:score/final scoring)))))
  (testing "Team data"
    (let [results (q '[:find ?t :where
                       [?t :team/mascot]] (db conn))]
      (is (= 32 (count results)))
      (doseq [result results]
        (let [team (d/touch (d/entity (db conn) (first result)))]
          (is (:team/abbr team))
          (is (:team/name team))
          (is (:team/mascot team))
          (is (:db/id team)))))
    (is (= "Packers"
           (:team/mascot
            (d/entity
             (db conn)
             (get-team-id (db conn) "GB"))))))
  (testing "Player data"
    (let [results (q '[:find ?p :where [?p :player/nflid]] (db conn))]
      (is (= 55 (count results)))))

  (testing "Rushing stats"
    (let [results (q '[:find ?r :where [_ :home/rushing ?r]] (db conn))]
      (is (= (count results) 4)))
    (let [results (q '[:find ?r :where [?r :rushing/yards]] (db conn))]
      (is (= (count results) 8)))
    (test-player-stats
     :away/rushing
     [:rushing/yards
      :rushing/attempts
      :rushing/tds
      :rushing/long
      :rushing/longtd
      :rushing/twopta
      :rushing/twoptm]))

  (testing "Passing stats"
    (test-player-stats
     :away/passing
     [:passing/attempts
      :passing/complete
      :passing/yards
      :passing/tds
      :passing/ints
      :passing/twopta
      :passing/twoptm]))

  (testing "Receiving stats"
    (test-player-stats
     :away/receiving
     [:receiving/receptions
      :receiving/yards
      :receiving/tds
      :receiving/long
      :receiving/longtd
      :receiving/twopta
      :receiving/twoptm]))

  (testing "Kicking stats"
    (test-player-stats 
     :away/kicking 
     [:kicking/fg-attempts
      :kicking/fg-made
      :kicking/xp-attempts
      :kicking/xp-made]))

  (testing "Defensive stats"
    (test-player-stats
     :away/defense
     [:defense/sacks
      :defense/ints
      :defense/tackles
      :defense/assists
      :defense/fumbles-forced]))

  (testing "Fumble stats"
    (test-player-stats
     :away/fumbles
     [:fumbles/lost
      :fumbles/recovered
      :fumbles/total
      :fumbles/trcv]))

  (testing "Punt return stats"
    (test-player-stats
     :away/puntret
     [:puntret/returns
      :puntret/tds
      :puntret/long
      :puntret/avg
      :puntret/longtd]))

  (testing "Kick return stats"
    (test-player-stats
     :away/kickret
     [:kickret/returns
      :kickret/tds
      :kickret/long
      :kickret/avg
      :kickret/longtd])))

(deftest test-get-save-directory-default
  (is (= "./stats" (get-save-directory))))

#_(run-tests)
