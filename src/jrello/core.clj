(ns jrello.core
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

(defn get-tickets [] (let [secrets (get-secrets)
                           req (str "https://api.trello.com/1/boards/" board-id "/cards?key=" (get secrets :key) "&token=" (get secrets :token))
                           resp (client/get req {:cookie-policy :none})]
                       (json/read-str (get resp :body) :key-fn keyword)))

(defn labels [tickets] (->> tickets
                            (map (fn [ticket] (get ticket :labels)))
                            (flatten)
                            (map (fn [label] (get label :name)))
                            (set)))



(def tickets (get-tickets))

(labels tickets)

(defn -main
  "Getting tickets. Please hold..."
  [& args]
  (clojure.pprint/pprint (get-tickets)))
