(ns jrello.trello.trello-service
  (:require [jrello.trello.trello-api :as trello-api]
            [clojure.java.io :as io]
            [clojure.data.csv :as csv]
            [clojure.string :as string]
            [jrello.config :as config])
  (:import (java.time Instant)
           (java.time DayOfWeek Duration Year ZoneId ZoneOffset ZonedDateTime)
           (java.time.format DateTimeFormatter)
           (java.time.temporal IsoFields)
           (org.threeten.extra YearWeek)))

(def UTC "UTC")
(def NO-DAYS-IN-WEEK 7)
(def NO-WEEKEND-DAYS 2)

(def DEFAULT-CYCLE-TIME 4.00)

(def exclude-labels-in-progress #{"sendle", "sc2", "sendle locations", "hubbed api", "🐛 bug", "refactoring", "enhancement 💡", "sendle-tracking", "sendle-frontend", "sendle-locations"})
(def exclude-labels #{"sendle", "sc2", "sendle locations", "hubbed api", "refactoring", "sendle-tracking", "sendle-frontend", "sendle-locations"})

(def get-trello-boards
  (let [{:keys [trello-boards]} (config/read-system-config)]
    trello-boards))

(defn- format-date [input]
  (Instant/parse input))

(defn- format-double-2dp [input]
  (Double/parseDouble (format "%.2f" input)))

(defn- format-board-name [name]
  (-> (string/lower-case name)
      (.replaceAll " " "-")))

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
    (-> (+ day-without-weekends
           (if (= start-day-of-week DayOfWeek/SUNDAY) 1 0)
           (if (= end-day-of-week DayOfWeek/SUNDAY) 1 0))
        format-double-2dp)))

(comment
  (calculate-cycle-time "2024-02-12T04:06:48.333Z" "2024-02-16T05:19:39.275Z")
  (calculate-cycle-time "2024-01-29T00:08:16.493Z" "2024-02-07T02:30:51.192Z"))

(defn- get-date-of-list-update [actions list-id]
  (-> (filter #(and (= (:type %) "updateCard")
                    (= (get-in % [:data :listAfter :id]) list-id)) actions)
      first :date))

(defn- get-created-date [card-id]
  (let [instant (-> (Integer/parseInt (subs card-id 0 8) 16)
                    (* 1000)
                    (Instant/ofEpochMilli))
        zdt (ZonedDateTime/ofInstant instant ZoneOffset/UTC)
        formatter (DateTimeFormatter/ofPattern "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")]
    (.format formatter zdt)))

(defn- get-week-of-year [date-time]
  (-> (format-date date-time)
      (ZonedDateTime/ofInstant ZoneOffset/UTC)
      (.get (IsoFields/WEEK_OF_WEEK_BASED_YEAR))))

(defn get-start-of-week-date [week]
  (let [current-year (.getValue (Year/now))]
    (-> (YearWeek/of current-year week) (.atDay DayOfWeek/MONDAY))))

(defn- build-completed-card-data-map [trello-lists {:keys [id] :as card}]
  (let [actions (trello-api/get-card-actions id)
        in-progress-date-time (or (get-date-of-list-update actions (:in-progress trello-lists))
                                  (get-date-of-list-update actions (:qa trello-lists))
                                  (get-created-date id))
        done-date-time (get-date-of-list-update actions (:done trello-lists))
        default-label "Enhancement 💡"
        week-of-year (get-week-of-year done-date-time)]
    (-> (select-keys card [:id :name :idList])
        (assoc :label-name (or (-> (filter #(not (contains? exclude-labels (string/lower-case (:name %)))) (:labels card))
                                   first :name)
                               default-label)
               :in-progress in-progress-date-time
               :done done-date-time
               :cycle-time (calculate-cycle-time in-progress-date-time done-date-time)
               :week-of-year week-of-year
               :week-start-date (get-start-of-week-date week-of-year)))))

(defn- get-cards-in-done [{:keys [trello-lists]}]
  (let [cards (trello-api/get-cards-in-list (:done trello-lists))]
    (map #(build-completed-card-data-map trello-lists %) cards)))

(defn- average-cycle-time [cards]
  (if (> (count cards) 0)
    (-> (/ (apply + (map :cycle-time cards))
         (count cards))
        format-double-2dp)
    DEFAULT-CYCLE-TIME))

(defn- cards-completed-by-label [group-by-label-name]
  (reduce (fn [agg label]
            (let [types (get group-by-label-name label)]
              (conj agg (str label ": " (count types)))))
          [] (sort (keys group-by-label-name))))

(defn cards-completed-by-week [cards]
  (let [group-by-week (group-by :week-of-year cards)]
    (reduce (fn [stats week-as-number]
              (let [completed-week (get group-by-week week-as-number)]
                (assoc stats week-as-number (hash-map :count (count completed-week)
                                                      :by-type (cards-completed-by-label (group-by :label-name completed-week))))))
            {} (keys group-by-week))))

(defn get-stats-for-completed-cards [trello-board]
  (let [cards (get-cards-in-done trello-board)]
    {:single-stats       {:completed-cards (count cards)
                          :avg-cycle-time  (average-cycle-time cards)}
     :completed-per-week (cards-completed-by-week cards)
     :card-stats         cards}))

(defn- now-as-date-string []
  (-> (Instant/now)
      str
      (subs 0 10)))

(defn save-stats-for-completed-cards [{:keys [name] :as trello-board} card-stats]
  (let [csv-filename (format "%s-trello-stats-%s.csv" (format-board-name name) (now-as-date-string))]
    (with-open [writer (io/writer csv-filename)]
      (csv/write-csv writer (concat [(keys (first card-stats))] ;;create csv header
                                    (map vals (sort-by :done card-stats)))))
    csv-filename))


(defn- build-awaiting-card-data-map [{:keys [labels] :as card}]
  (-> (select-keys card [:id :name :idList])
      (assoc :project (-> (filter #(not (contains? exclude-labels-in-progress (string/lower-case (:name %)))) labels)
                          first :name))))

(defn- forecast-days-to-completion
  ([cards-outstanding avg-cycle-time]
   (-> (* cards-outstanding avg-cycle-time) format-double-2dp))
  ([cards-outstanding avg-cycle-time number-of-people]
   (let [cards-by-people (quot cards-outstanding number-of-people)]
     (if (= cards-by-people 0)
       avg-cycle-time
       (-> (+ (* (quot cards-outstanding number-of-people) avg-cycle-time)
              (* (rem cards-outstanding number-of-people) avg-cycle-time))
           format-double-2dp)))))

(defn get-cards-awaiting-completion [{:keys [trello-lists]}]
  (let [cards (concat (trello-api/get-cards-in-list (:to-do trello-lists))
                      (trello-api/get-cards-in-list (:in-progress trello-lists))
                      (trello-api/get-cards-in-list (:qa trello-lists)))]
    (map #(build-awaiting-card-data-map %) cards)))

(defn get-stats-for-cards-awaiting-completion [trello-board avg-cycle-time]
  (let [cards (get-cards-awaiting-completion trello-board)
        cards-by-project (group-by :project cards)
        projects (remove nil? (keys cards-by-project))]
    (reduce (fn [stats project]
              (let [cards-outstanding (count (get cards-by-project project))]
                (conj stats (hash-map :name project
                                      :cards cards-outstanding
                                      :forecast (forecast-days-to-completion cards-outstanding avg-cycle-time)
                                      :forecast-two-people (forecast-days-to-completion cards-outstanding avg-cycle-time 2)
                                      :forecast-three-people (forecast-days-to-completion cards-outstanding avg-cycle-time 3)))))
            [] projects)))

(comment
  ;; write the card data to csv for analysis
  (with-open [writer (io/writer "out-file.csv")]
    (let [done-cards (get-cards-in-done (first get-trello-boards))]
      (csv/write-csv writer (concat
                              [(keys (first done-cards))]
                              (map vals done-cards)))))
)