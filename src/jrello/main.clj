(ns jrello.main
  (:gen-class)
  (:require [clj-http.client :as client]
            [clojure.java.io :as io]
            [clojure.edn :as edn]
            [clojure.string]
            [clojure.data.json :as json]))

;; Load API creds from secrets.edn

(defn load-edn
  "Load edn from an io/reader source (filename or io/resource)."
  [source]
  (try
    (with-open [r (io/reader source)]
      (edn/read (java.io.PushbackReader. r)))
    (catch java.io.IOException e
      (printf "Couldn't open '%s': %s\n" source (.getMessage e)))
    (catch RuntimeException e
      (printf "Error parsing edn file '%s': %s\n" source (.getMessage e)))))

(defn get-secrets [] (load-edn "secrets.edn"))

;; Get tickets and lists for a given Trello board

(defn tickets-raw [board-id] (let [secrets (get-secrets)
                                   req (str "https://api.trello.com/1/boards/" board-id "/cards?key=" (get secrets :key) "&token=" (get secrets :token))
                                   resp (client/get req {:cookie-policy :none})]
                               (json/read-str (get resp :body) :key-fn keyword)))

(defn board-lists-raw [board-id] (let [secrets (get-secrets)
                                       req (str "https://api.trello.com/1/boards/" board-id "/lists?key=" (get secrets :key) "&token=" (get secrets :token))
                                       resp (client/get req {:cookie-policy :none})]
                                   (json/read-str (get resp :body) :key-fn keyword)))

;; Clean and Transform trello data 

(defn clean-board-lists
  "Generates a map of board list id => board list name"
  [board-lists]
  (zipmap
   (map :id board-lists)
   (map :name board-lists)))

(defn clean-tickets
  "Generates a map of ticket id => ticket"
  [tickets board-lists complete-list-id]
  (map (fn [ticket] (merge
                     (select-keys ticket [:name :id :lastActivity])
                     {:list (get board-lists (:idList ticket))}
                     {:done (= (:idList ticket) complete-list-id)}
                     {:labels (map :name (:labels ticket))}))
       tickets))

;; Report

(defn report-on-tickets
  "Given a list of tickets will generate a summary report"
  [tickets title]
  (let [percent-complete (float (/ (count (filter #(:done %) tickets)) (count tickets)))
        labels (map #(str (:list %) " - " (:name %)) tickets)]
    (str "Report for " title ". " percent-complete "% Complete\n" (clojure.string/join "\n" labels))))

(defn report-by-label
  "Generate a report by label"
  [tickets]
  (let [tickets-by-list (group-by :labels tickets)]
    (map (fn [list] (report-on-tickets (get tickets-by-list list) (first list)))
         (keys tickets-by-list))))

(defn report-by-list
  "Generate a report by board list"
  [tickets]
  (let [tickets-by-list (group-by :list tickets)]
    (map (fn [list] (str list ": " (count (get tickets-by-list list)) " tickets"))
         (keys tickets-by-list))))



;; main

(defn -main
  "Getting tickets. Please hold..."
  [& args]
  (def board-id "ZuuyPheS")
  (def cleaned-tickets (let [cleanded-board-lists (clean-board-lists (board-lists-raw board-id))] (clean-tickets
                                                                                                   (tickets-raw board-id)
                                                                                                   cleanded-board-lists
                                                                                                   (first (last cleanded-board-lists)))))
  (def divider-main "\n================================\n")
  (def divider      "\n--------------------------------\n")
  (println divider-main)
  (println "Progress report by list")
  (println divider-main)
  (println (clojure.string/join "\n" (report-by-list cleaned-tickets)))
  (println divider-main)
  (println "Progress report by label/project")
  (println divider-main)
  (println (clojure.string/join divider (report-by-label cleaned-tickets))))
