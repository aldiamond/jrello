(ns jrello.config
  (:require [kit.config :as config]))

(def ^:const system-filename "system.edn")

(def ^:private system-config (atom {}))

(defn read-system-config []
  (when (empty? @system-config)
    (reset! system-config (config/read-config system-filename {})))
  @system-config)