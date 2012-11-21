(ns football-stats.nfldotcom.storage-test
  (:use clojure.test
        football-stats.nfldotcom.storage
        [football-stats.nfldotcom.schema :only [install]]
        [datomic.api :only [q db] :as d]))

(def ^:dynamic conn)

(def datomic-test-uri "datomic:mem://test")

(def nflgame (read-string (slurp "test/data/2011090800")))


(defn datomic-fixture [t]
  ;; Setup
  (d/delete-database datomic-test-uri)
  (d/create-database datomic-test-uri)
  (binding [conn (d/connect datomic-test-uri)]
    (install conn)
    (store-game conn nflgame)
    (t)) ;; execute test
  )

(use-fixtures :once datomic-fixture)

(defn find-game []
  (q '[:find ?g :where
       [?g :game/gameid "2011090800"]] (db conn)))

(deftest test-store-game
  (is (= 1 (count (find-game)))))

(defn get-game []
  (d/entity (db conn) (first (first (find-game)))))

(deftest test-home-team
  (let [home-team (:home/team (get-game))]
    (is (= "GB" (:team/abbr home-team)))))

(deftest test-home-scoring
  (let [scoring (:home/score (get-game))]
    (is (= 21 (:score/q1 scoring)))
    (is (= 7 (:score/q2 scoring)))
    (is (= 7 (:score/q3 scoring)))
    (is (= 7 (:score/q4 scoring)))
    (is (= 0 (:score/ot scoring)))
    (is (= 42 (:score/final scoring)))))

(deftest test-team-data
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

(deftest test-player-data
  (let [results (q '[:find ?p :where [?p :player/nflid]] (db conn))]
    (is (= 55 (count results)))))

(defn test-player-stats 
  [parent-key stat-keys]
  (let [results (q `[:find ?p :where [?x ~parent-key ?p]] (db conn))
        stat-line (d/entity (db conn) (first (first results)))]
    (is (< 0 (count results)))
    (is (-> stat-line :stats/player :player/name nil? not))
    (for [k stat-keys]
      (is (number? (k stat-line))))))

(deftest test-rushing
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

(deftest test-passing
  (test-player-stats
    :away/passing
    [:passing/attempts
     :passing/complete
     :passing/yards
     :passing/tds
     :passing/ints
     :passing/twopta
     :passing/twoptm]))

(deftest test-receiving
  (test-player-stats
    :away/receiving
    [:receiving/receptions
     :receiving/yards
     :receiving/tds
     :receiving/long
     :receiving/longtd
     :receiving/twopta
     :receiving/twoptm]))

(deftest test-kicking
  (test-player-stats 
    :away/kicking 
    [:kicking/fg-attempts
     :kicking/fg-made
     :kicking/xp-attempts
     :kicking/xp-made]))

(deftest test-defense 
  (test-player-stats
    :away/defense
    [:defense/sacks
     :defense/ints
     :defense/tackles
     :defense/assists
     :defense/fumbles-forced]))

(deftest test-fumbles
  (test-player-stats
    :away/fumbles
    [:fumbles/lost
     :fumbles/recovered
     :fumbles/total
     :fumbles/trcv]))

(deftest test-puntret
  (test-player-stats
    :away/puntret
    [:puntret/returns
     :puntret/tds
     :puntret/long
     :puntret/avg
     :puntret/longtd]))

(deftest test-kickret
  (test-player-stats
    :away/kickret
    [:kickret/returns
     :kickret/tds
     :kickret/long
     :kickret/avg
     :kickret/longtd]))