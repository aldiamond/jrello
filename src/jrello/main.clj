(ns jrello.main
  (:gen-class)
  (:require [clojure.string :as string]
            [clojure.string]
            [clojure.tools.cli :as cli]
            [jrello.trello.trello-service :as trello-service]))

(def MAIN-DIVIDER "\n================================\n")

(def stat-labels {:avg-cycle-time  "Average cycle time"
                  :completed-cards "Cards completed (last 60 days)"})

(def argument-options [["-s" "--save-csv" "Save CSV of completed card Trello data" :default false]])

(defn- save-csv! [trello-board card-stats]
  (let [card-stats-csv-filename (trello-service/save-stats-for-completed-cards trello-board card-stats)]
    (println (format "Saving completed card stats in: %s" card-stats-csv-filename))))

(defn- get-and-print-stats [{:keys [name] :as trello-board} {:keys [save-csv]}]
  (println MAIN-DIVIDER)
  (println (format "Trello Board: %s" name))
  (println "Getting trello card details. Please hold....")
  (let [{:keys [single-stats completed-per-week card-stats]} (trello-service/get-stats-for-completed-cards trello-board)
        cards-awaiting-completion-stats (trello-service/get-stats-for-cards-awaiting-completion trello-board (:avg-cycle-time single-stats))]

    (when save-csv
      (save-csv! trello-board card-stats))

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

(defn- print-help [{:keys [summary errors]}]
  (when errors
    (println "Errors:")
    (println errors))
  (println "\nSummary:")
  (println summary))

(defn -main
  "Getting tickets. Please hold..."
  [& args]
  (let [{:keys [options errors] :as parsed-opts} (cli/parse-opts args argument-options)]
    (println "Parsed options: " options)
    (if (nil? errors)
      (doseq [trello-board trello-service/get-trello-boards]
        (get-and-print-stats trello-board options))
      (print-help parsed-opts))))