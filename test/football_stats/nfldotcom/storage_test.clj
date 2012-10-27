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
  (d/create-database datomic-test-uri)
  (binding [conn (d/connect datomic-test-uri)]
    (install conn)
    (store-game nflgame conn)
    (t)) ;; execute test
  ;; Tear down
  (d/delete-database datomic-test-uri))

(use-fixtures :once datomic-fixture)

(defn find-game []
  (q '[:find ?g :where
       [?g :game/gameid "2011090800"]] (db conn)))

(deftest test-store-game
  (is (= 1 (count (find-game)))))

(defn get-game []
  (d/entity (db conn) (first (first (find-game)))))

(deftest test-home-team
  (let [home-team (:game/home (get-game))]
    (is (= "GB" (:game.team/abbr home-team)))
    (is (= 1 (:game.team/to home-team)))))

(deftest test-home-scoring
  (let [scoring (-> (get-game) :game/home :game.team/score)]
    (is (= 21 (:score/q1 scoring)))
    (is (= 7 (:score/q2 scoring)))
    (is (= 7 (:score/q3 scoring)))
    (is (= 7 (:score/q4 scoring)))
    (is (= 0 (:score/ot scoring)))
    (is (= 42 (:score/final scoring)))))

(comment
  (deftest test-home-rushing-stats
   (let [rushing-stats (-> (get-game)
                           :game/home
                           :game.team/stats
                           :stats/rushing)]
     (is (= 999 rushing-stats)))))

(deftest test-team-data
  (doseq [e (q '[:find ?t :where
                 [?t :team/mascot]])]
    (let [team (d/touch (d/entity e))]
      (is (:team/abbr team))
      (is (:team/name team))
      (is (:team/mascot team))
      (is (:db/id team)))))