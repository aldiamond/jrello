(ns jrello.main
  (:gen-class)
  (:require [clj-http.client :as client]
            [clojure.java.io :as io]
            [clojure.edn :as edn]
            [clojure.data.json :as json]))

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

(def board-id "ZuuyPheS")

(defn get-tickets [board-id] (let [secrets (get-secrets)
                                   req (str "https://api.trello.com/1/boards/" board-id "/cards?key=" (get secrets :key) "&token=" (get secrets :token))
                                   resp (client/get req {:cookie-policy :none})]
                               (json/read-str (get resp :body) :key-fn keyword)))

;; Need the following datasets
;; 1. a map of ticket id to a ticket + status
;; 2. a map of label id to ticket ids
;; 3. a set of [label names, label ids]
;; Then just loop through the labels and print the each ticket status

(defn group-tickets [tickets] (->> tickets
                                   (map (fn [ticket] {:name (get ticket :name),
                                                      :labels (map :name (get ticket :labels))}))
                                   (group-by #(-> % :labels))))

(def tickets (get-tickets board-id))
(group-tickets tickets)

(defn -main
  "Getting tickets. Please hold..."
  [& args]
  (clojure.pprint/pprint (group-tickets tickets)))
