(ns jrello.main
  (:gen-class)
  (:require [clojure.string :as string]
            [clojure.string]
            [jrello.trello.trello-service :as trello-service])
  (:import (java.time DayOfWeek)
           (org.threeten.extra YearWeek)))

(def MAIN-DIVIDER "\n================================\n")

(def stat-labels {:avg-cycle-time  "Average cycle time"
                  :completed-cards "Cards completed (last 40 days)"})

(defn- get-and-print-stats []
  (println MAIN-DIVIDER)
  (println "Getting trello card details. Please hold....")
  (let [{:keys [single-stats completed-per-week]} (trello-service/get-stats-for-completed-cards)
        cards-awaiting-completion-stats (trello-service/get-stats-for-cards-awaiting-completion (:avg-cycle-time single-stats))]

    (println MAIN-DIVIDER)
    (println "Completed card stats\n")
    (doseq [stat-kw (keys stat-labels)]
      (println (format "%30s: %s" (stat-kw stat-labels) (stat-kw single-stats))))

    (println "\nCards completed per week:")
    (doseq [week (sort > (keys completed-per-week))]
      (let [week-monday (trello-service/get-start-of-week-date week)
            {:keys [count by-type]} (get completed-per-week week)]
        (println (format "%5s: %s [%s]" week-monday count (string/join ", " by-type)))))

    (println MAIN-DIVIDER)
    (println "Projects awaiting completion\n")
    (doseq [{:keys [name cards forecast forecast-two-people forecast-three-people]} cards-awaiting-completion-stats]
      (println (format "%20s: %5s cards ~ %5s days one person | %5s days two people | %5s days three people" name cards forecast forecast-two-people forecast-three-people)))))

(defn -main
  "Getting tickets. Please hold..."
  [& args]
  ;;TODO:// select board
  (get-and-print-stats))