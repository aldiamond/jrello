(ns jrello.main
  (:gen-class)
  (:require [clojure.string]
            [jrello.trello.trello-service :as trello-service]))

(def MAIN-DIVIDER "\n================================\n")

(def stat-labels {:avg-cycle-time  "Average cycle time"
                  :completed-cards "Cards completed (last 40 days)"})

(defn- get-and-print-stats []
  (println MAIN-DIVIDER)
  (println "Getting trello card details. Please hold....\n")
  (let [completed-card-stats (trello-service/get-stats-for-completed-cards)
        cards-awaiting-completion-stats (trello-service/get-stats-for-cards-awaiting-completion completed-card-stats)]

    (println MAIN-DIVIDER)
    (println "Completed card stats\n")
    (doseq [stat-kw (keys stat-labels)]
      (println (format "%30s: %s" (stat-kw stat-labels) (stat-kw completed-card-stats))))

    (println MAIN-DIVIDER)
    (println "Projects awaiting completion\n")
    (doseq [{:keys [name cards forecast forecast-two-people]} cards-awaiting-completion-stats]
      (println (format "%20s: %5s cards ~ %5s days one person | %5s days two people" name cards forecast forecast-two-people)))))

(defn -main
  "Getting tickets. Please hold..."
  [& args]
  ;;TODO:// select board
  (get-and-print-stats))
