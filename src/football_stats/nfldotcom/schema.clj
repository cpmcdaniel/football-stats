(ns football-stats.nfldotcom.schema
  (:use [datomic.api :only [q db] :as d]
        football-stats.nfldotcom.storage
        [clojure.java.io :only [resource]]
        [cheshire.core :only [parse-string]]))

(defn get-static-team-info []
  (parse-string
   (slurp
    (resource "football_stats/nfldotcom/team-data.json"))
   true))

(defn create-metadata-txs []
  ;; Transaction data for team info.
  (for [[abbr team] (get-static-team-info)]
    {:db/id (d/tempid :db.part/user)
     :team/abbr (:abbr team)
     :team/name (:city team)
     :team/mascot (:nickname team)}))

(defn install-metadata [conn]
  (do
    (d/transact conn (create-metadata-txs))))

(defn install-schema [conn]
  (d/transact
   conn
   (read-string
    (slurp
     (resource "football_stats/nfldotcom/schema.dtm")))))

(defn install [conn]
  (do
    (install-schema conn)
    (install-metadata conn)))


(defn test-db []
  (let [uri "datomic:mem://test"
        conn (do 
               (d/delete-database uri)
               (d/create-database uri)
               (d/connect uri))
        mydb (do
               (install conn)
               (db conn))]
    (d/touch (d/entity mydb (first (first (q '[:find ?t :where [?t :team/abbr "GB"]] mydb)))))))

(comment
  (create-metadata-txs))