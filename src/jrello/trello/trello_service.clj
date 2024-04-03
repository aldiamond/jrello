(ns jrello.trello.trello-service
  (:require [jrello.trello.trello-api :as trello-api]
            [clojure.java.io :as io]
            [clojure.data.csv :as csv]
            [clojure.string :as string]
            [jrello.config :as config])
  (:import (java.time Instant)
           (java.time DayOfWeek Duration ZoneId ZoneOffset ZonedDateTime)
           (java.time.format DateTimeFormatter)))

(def UTC "UTC")
(def NO-DAYS-IN-WEEK 7)
(def NO-WEEKEND-DAYS 2)

(def exclude-labels #{"sendle", "sc2", "sendle locations", "hubbed api", "🐛 bug", "refactoring", "enhancement 💡"})

(def get-trello-lists
  (let [{:keys [trello-lists]} (config/read-system-config)]
    trello-lists))

(defn- format-date [input]
  (Instant/parse input))

(defn- format-double-2dp [input]
  (Double/parseDouble (format "%.2f" input)))

(defn- days-between-two-instants [start-instant end-instant]
  (-> (Duration/between start-instant end-instant)
      (.toMillis)
      (/ (* 1000 60 60 24))
      double
      format-double-2dp))

(defn- get-instant-day-of-week [instant]
  (-> (.atZone instant (ZoneId/of UTC)) (.getDayOfWeek)))

(defn- calculate-cycle-time [in-progress-date-time done-date-time]
  (let [in-progress-instant (format-date in-progress-date-time)
        done-instant (format-date done-date-time)
        start-day-of-week (get-instant-day-of-week in-progress-instant)
        end-day-of-week (get-instant-day-of-week done-instant)
        days (days-between-two-instants in-progress-instant done-instant)
        day-without-weekends (- days
                                (* (quot (+ days (.getValue start-day-of-week)) NO-DAYS-IN-WEEK)
                                   NO-WEEKEND-DAYS))]
    (+ day-without-weekends
       (if (= start-day-of-week DayOfWeek/SUNDAY) 1 0)
       (if (= end-day-of-week DayOfWeek/SUNDAY) 1 0))))

(comment
  (calculate-cycle-time "2024-02-12T04:06:48.333Z" "2024-02-16T05:19:39.275Z")
  (calculate-cycle-time "2024-01-29T00:08:16.493Z" "2024-02-07T02:30:51.192Z"))

(defn- get-date-of-list-update [actions status-kw]
  (-> (filter #(and (= (:type %) "updateCard")
                    (= (get-in % [:data :listAfter :id]) (status-kw get-trello-lists))) actions)
      first :date))

(defn- get-created-date [card-id]
  (let [instant (-> (Integer/parseInt (subs card-id 0 8) 16)
                    (* 1000)
                    (Instant/ofEpochMilli))
        zdt (ZonedDateTime/ofInstant instant ZoneOffset/UTC)
        formatter (DateTimeFormatter/ofPattern "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")]
    (.format formatter zdt)))

(defn- build-completed-card-data-map [{:keys [id] :as card}]
  (let [actions (trello-api/get-card-actions id)
        in-progress-date-time (or (get-date-of-list-update actions :in-progress)
                                  (get-date-of-list-update actions :qa)
                                  (get-created-date id))
        done-date-time (get-date-of-list-update actions :done)]
    (-> (select-keys card [:id :name :idList])
        (assoc :idLabel (-> card :idLabels first)
               :in-progress in-progress-date-time
               :done done-date-time
               :cycle-time (calculate-cycle-time in-progress-date-time done-date-time)))))

(defn- get-cards-in-done []
  (let [cards (trello-api/get-cards-in-list (:done get-trello-lists))]
    (map build-completed-card-data-map cards)))

(defn- average-cycle-time [cards]
  (-> (/ (apply + (map :cycle-time cards))
         (count cards))
      format-double-2dp))

(defn get-stats-for-completed-cards []
  (let [cards (get-cards-in-done)]
    {:completed-cards (count cards)
     :avg-cycle-time  (average-cycle-time cards)}))

(defn- build-awaiting-card-data-map [{:keys [labels] :as card}]
  (-> (select-keys card [:id :name :idList])
      (assoc :project (-> (filter #(not (contains? exclude-labels (string/lower-case (:name %)))) labels)
                          first :name))))

(defn- forecast-days-to-completion
  ([cards-outstanding avg-cycle-time]
   (-> (* cards-outstanding avg-cycle-time) format-double-2dp))
  ([cards-outstanding avg-cycle-time number-of-people]
   (-> (+ (* (quot cards-outstanding number-of-people) avg-cycle-time)
          (* (rem cards-outstanding number-of-people) avg-cycle-time))
       format-double-2dp)))

(defn get-cards-awaiting-completion []
  (let [cards (concat (trello-api/get-cards-in-list (:to-do get-trello-lists))
                      (trello-api/get-cards-in-list (:in-progress get-trello-lists))
                      (trello-api/get-cards-in-list (:qa get-trello-lists)))]
    (map #(build-awaiting-card-data-map %) cards)))

(defn get-stats-for-cards-awaiting-completion [{:keys [avg-cycle-time]}]
  (let [cards (get-cards-awaiting-completion)
        cards-by-project (group-by :project cards)
        projects (remove nil? (keys cards-by-project))]
    (reduce (fn [stats project]
              (let [cards-outstanding (count (get cards-by-project project))]
                (conj stats (hash-map :name project
                                      :cards cards-outstanding
                                      :forecast (forecast-days-to-completion cards-outstanding avg-cycle-time)
                                      :forecast-two-people (forecast-days-to-completion cards-outstanding avg-cycle-time 2)))))
            [] projects)))

(comment
  ;; write the card data to csv for analysis
  (with-open [writer (io/writer "out-file.csv")]
    (csv/write-csv writer (map vals (get-cards-in-done))))
)