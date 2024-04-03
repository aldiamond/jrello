(ns jrello.trello.trello-api
  (:require [clj-http.client :as client]
            [clojure.data.json :as json]
            [jrello.config :as config]))

(def trello-endpoint "https://api.trello.com/1/")

(defn- build-trello-get-api-url [path]
  (let [{:keys [trello-cfg]} (config/read-system-config)]
    (format "%s?key=%s&token=%s" (str trello-endpoint path) (:key trello-cfg) (:token trello-cfg))))

(defn- format-response-body [{:keys [body]}]
  (json/read-str body :key-fn keyword))

(defn- get-api-call
  ([path]
   (-> (client/get (build-trello-get-api-url path) {:cookie-policy :none})
       format-response-body)))

(defn get-cards-in-list [list-id]
  (get-api-call (str "lists/" list-id "/cards")))

(defn get-card-actions [card-id]
  (let [path (str "cards/" card-id "/actions")]
    (-> (client/get (build-trello-get-api-url path) {:cookie-policy :none})
        format-response-body)))