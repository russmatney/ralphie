(ns ralphie.pomodoro
  (:require
   [ralphie.command :refer [defcom]]
   [ralphie.awesome :as awm]
   [ralphie.config :as config]
   [clojure.java.shell :as sh]))

(defn update-pomodoro-widget [msg]
  (->> msg
       (awm/awm-fn "update_pomodoro_widget")
       awm/awm-cli))

(comment
  (update-pomodoro-widget {:label "label"
                           :value "value"}))

(defn update-pomodoro-widget-handler
  [_config _parsed]
  ;; wip does nothing for now
  (sh/sh "echo" "\n\n" :out (config/pomodoros-file)))

(defcom update-pomodoro-widget-cmd
  {:name          "update-pomodoro-widget"
   :one-line-desc "Updates the minutes passed on the pomodoro-widget"
   :description   [""]
   :handler       update-pomodoro-widget-handler})
