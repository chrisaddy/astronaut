(ns astronaut.core
  (:require [cli-matic.core :refer [run-cmd]])
  (:require [clojure.java.jdbc :refer :all])
  (:require [clojure.pprint :as p])
  (:require [clojure.java.jdbc :as j])
  (:require [clj-time.core :as t])
  (:require [clj-time.coerce :as c])
  (:require [clojure.java.io :as io])
  (:gen-class))

(def home-directory (System/getProperty "user.home"))
(def astro-directory (str home-directory "/.astronaut/"))
(def cards-db-location (str astro-directory "cards.db"))
(def INITIALIZE-DB (str "create table cards"
                        "(id INTEGER PRIMARY KEY,"
                        "card_id INTEGER,"
                        "front TEXT,"
                        "back TEXT,"
                        "type TEXT,"
                        "tags TEXT,"
                        "attempt INTEGER,"
                        "confidence INTEGER,"
                        "review INTEGER,"
                        "next_attempt INTEGER,"
                        "next_review INTEGER);"))

(defn current-time
  []
  (c/to-long (t/now)))

(def db
  {:classname   "org.sqlite.JDBC"
   :subprotocol "sqlite"
   :subname     cards-db-location})

(defn qry
  "Return the result of a query from a string when the result is expected to be a single value"
  [q]
  (first (j/query db q)))

(defn count-cards
  "Return the current number of cards in the database"
  []
  (:count (qry "select count(*) as count from cards;")))

(defn largest-card-id
  "Find the largest card id, if none exists, return 0"
  []
  (:max (qry "select max(card_id) as max from cards;")))

(defn new-card-id
  "Create a new integer card id by incrementing the largest id"
  []
  (+ 1 (largest-card-id)))

(defn new-id
  [{card_id :card_id attempt :attempt}]
  (Integer/parseInt (str card_id attempt)))

(defn review-card
  [{front :front, back :back, attempt :attempt card_id :card_id}]
  (println (str "\u001b[32;1m" front "\u001b[0m\n"))
  (println "press any key when ready to flip card")
  (def pause (read-line))
  (println (str "\u001b[34;1m" back "\u001b[0m")
  (println "How easy was that?")
  (def confidence (read-line)))
  (schedule {:attempt attempt :card_id card_id :confidence confidence})
  (println ""))

(defn review
  []
  (def ts (current-time))
  (def cards (j/query db (str "select * from cards where next_review >" ts " order by next_review DESC limit 20;")))
  (let [len (count cards)]
    (if (= len 0)
      (println "Congrats you're all done for now!")
      (do
        (println (str "\u001b[33;1m" len " cards left to review.\u001b[0m\n"))
        (map review-card cards)))))

(defn add-card
  [{:keys [front back tags]}]
  (def card_id (new-card-id))
  (def attempt 0)
  (def id (new-id {:card_id card_id :attempt attempt}))
  (def card-type "basic")
  (def confidence 0)
  (def review (current-time))
  (def query-string (str
                      "insert into cards "
                      "(id,card_id,front,back,type,tags,attempt,review,confidence,next_attempt,next_review) "
                      "values("
                      id ",'"
                      card_id "','"
                      front "','"
                      back "','"
                      card-type "','"
                      tags "','"
                      attempt "','"
                      review "','"
                      confidence "','"
                      (+ 1 attempt) "','"
                      review "');"))
  (j/execute! db query-string)
  (println (str card-type " card " id " added to ship.")))

(defn list-cards
  []
  (p/print-table (j/query db (str "select * from cards;"))))

(defn init-table
  [{:keys [& args]}]
  (println (str "creating " astro-directory))
  (.mkdir (java.io.File. astro-directory))
  (println (str "creating " cards-db-location))
  (spit cards-db-location nil)
  (j/execute! db INITIALIZE-DB)
  (println "cards db initialized"))

(def CONFIGURATION
  {:app         {:command     "astronaut"
                 :description "Command-line spaced-repition"
                 :version     "0.0.1"}

   :global-opts [{:option  "--tags"
                  :short   "-t"
                  :as      "Card context"
                  :type    :string
                  :default ""}]

   :commands    [{:command     "init"
                  :description "Initialize astronaut cli"
                  :runs        init-table}
                 {:command     "add-card" :short "a"
                  :description "Adds a card to the backlog"
                  :opts        [{:option "--front"
                                 :short "-f"
                                 :as "Front of the card"
                                 :type :string :default ""}
                                {:option "--back"
                                 :short "-b"
                                 :as "Back of the card"
                                 :type :string
                                 :default ""}]
                  :runs        add-card}
                 {:command     "inspect"
                  :description "Review scheduled cards"
                  :runs        review}
                 {:command     "list"
                  :description "List all cards"
                  :runs        list-cards}
                 ]})

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (run-cmd args CONFIGURATION))
