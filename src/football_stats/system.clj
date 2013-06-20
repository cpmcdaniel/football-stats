(ns football-stats.system
  (:require [datomic.api :refer [db q] :as d]
            [clojure.tools.logging :refer [info error]]
            [football-stats.nfldotcom.schema :as schema]
            [football-stats.nfldotcom.storage :as storage]))

(defn system
  "Constructs an instance of the system, but does not start it."
  []
  {:datomic-uri "datomic:mem://football-stats"
   :status :initialized})

(defn start
  "Performs side effects to initialize the system, acquire resources,
  and start it running. Returns an updated instance of the system."
  [{:keys [datomic-uri] :as system}]
  (let [install-needed (d/create-database datomic-uri)
        c (d/connect datomic-uri)]
    (when install-needed
      (info "Installing datomic database" datomic-uri)
      (schema/install c))
    (merge (storage/create-storage-channels c)
           (assoc system
             :status :started
             :conn c))))

(defn stop
  "Performs side effects to shut down the system and release its
  resources. Returns an updated instance of the system."
  [{:keys [conn] :as system}]
  (when conn (d/release conn))
  (storage/close-storage-channels system)
  (dissoc (assoc system :status :stopped) :conn))
