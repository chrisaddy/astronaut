(ns astronaut.core
  (:require [cli-matic.core :refer [run-cmd]])
  (:require [clojure.java.jdbc :refer :all])
  (:require [clojure.pprint :as p])
  (:require [clojure.java.jdbc :as j])
  (:require [clj-time.core :as t])
  (:require [clj-time.coerce :as c])
  (:require [clojure.java.io :as io])
  (:require [clojure.string :as str])
  (:require [clojure.math.numeric-tower :as math])
  (:gen-class))

(def home-directory (System/getProperty "user.home"))
(def astro-directory (str home-directory "/.astronaut/"))
(def cards-db-location (str astro-directory "cards.db"))
(def INITIALIZE-DB (str "create table cards"
                        "(id TEXT PRIMARY KEY,"
                        "card_id INTEGER,"
                        "front TEXT,"
                        "back TEXT,"
                        "attempt INTEGER,"
                        "confidence INTEGER,"
                        "review INTEGER,"
                        "next_attempt INTEGER,"
                        "next_review INTEGER);"))

(defn review-query
  [ts]
  (str "select * from (select distinct * from (select * from cards order by next_review desc) group by card_id) where next_review <= " ts ";"))

(defn current-time [] (c/to-long (t/now)))

(def db
  {:classname   "org.sqlite.JDBC"
   :subprotocol "sqlite"
   :subname     cards-db-location})

(defn qry
  "Return the result of a query from a string when the result is expected to be a single value"
  [q]
  (first (j/query db q)))


(defn newest
  []
  (p/print-table (j/query db REVIEWQUERY)))


(defn count-cards
  "Return the current number of cards in the database"
  []
  (:count (qry "select count(*) as count from cards;")))

(defn largest-card-id
  "Find the largest card id, if none exists, return 0"
  []
  (def lgid (:max (qry "select max(card_id) as max from cards;")))
  (if (nil? lgid)
    0
    lgid))

(defn new-card-id
  "Create a new integer card id by incrementing the largest id"
  []
  (+ 1 (largest-card-id)))

(defn create-id
  []
  (first (str/split (str (java.util.UUID/randomUUID)) #"-")))

(defn insert-card-query-string
  [{id :id
    attempt :attempt
    card_id :card_id
    front :front
    back :back
    review :review
    next-review :next-review
    confidence :confidence}]
  (str "insert into cards"
       "(id,card_id,front,back,attempt,review,confidence,next_attempt,next_review) "
       "values('"
        (create-id) "',"
        card_id ",'"
        front "','"
        back "',"
        attempt ","
        review ","
        confidence ","
        (+ 1 attempt) ","
        next-review ");"))

(defn schedule
  [{id :id attempt :attempt card_id :card_id confidence :confidence ts :ts}]
  (def last-attempt (first (j/query db (str "select * from cards where id = '" id "';"))))
  (def last-attempt-confidence (:confidence last-attempt))
  (def front (:front last-attempt))
  (def back (:back last-attempt))
  (defn next-review
    [att conf, t]
    (+ 86400000 (* (math/expt (int att ) (Math/log conf))) t))
  (println last-attempt)

  (def query-string (insert-card-query-string {:attempt (+ 1 attempt)
                             :card_id card_id
                             :confidence confidence
                             :review ts
                             :front front
                             :back back
                             :next-review (next-review attempt confidence ts)
                             }))
  (j/execute! db query-string))

(defn review-card
  [{id :id front :front, back :back, attempt :attempt card_id :card_id}]
  (println (str "\u001b[32;1m" front "\u001b[0m\n"))
  (println "press any key when ready to flip card")
  (def pause (read-line))
  (println (str "\u001b[34;1m" back "\u001b[0m"))
  (do (print "How easy was that [0-10]? ") (flush) (def confidence (read-line)))
  (schedule {:id id :attempt attempt :card_id card_id :confidence (Integer/parseInt confidence) :ts (current-time)}))

(defn review
  []
  (def ts (current-time))
  (def cards (j/query db (review-query ts)))
  (let [len (count cards)]
    (if (= len 0)
      (println "Congrats you're all done for now!")
      (do
        (println (str "\u001b[33;1m" len " cards left to review.\u001b[0m\n"))
        (map review-card cards)))))

(defn add-card
  [{:keys [front back]}]
  (def card_id (new-card-id))
  (def attempt 0)
  (def id (create-id))
  (def confidence 0)
  (def review (current-time))
  (def query-string (str
                      "insert into cards "
                      "(id,card_id,front,back,attempt,review,confidence,next_attempt,next_review) "
                      "values('"
                      id "',"
                      card_id ",'"
                      front "','"
                      back "','"
                      attempt "','"
                      review "','"
                      confidence "','"
                      (+ 1 attempt) "','"
                      review "');"))
  (j/execute! db query-string)
  (println (str "card " id " added to ship.")))

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
   ; :global-opts [{:option  "tags"
   ;                :short   "t"
   ;                :as      "Card context"
   ;                :type    :string
   ;                :default ""}]
   :commands    [{:command     "init"
                  :description "Initialize astronaut cli"
                  :runs        init-table}
                 {:command     "add-card" :short "a"
                  :description "Adds a card to the backlog"
                  :opts        [{:option "front"
                                 :short "f"
                                 :as "Front of the card"}
                                {:option "back"
                                 :short "b"
                                 :as "Back of the card"}]
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
