(ns ralphie.pomodoro
  (:require
   [ralphie.command :refer [defcom]]
   [ralphie.awesome :as awm]))

(defn update-pomodoro-widget [msg]
  (->> msg
       (awm/awm-fn "update_pomodoro_widget")
       awm/awm-cli))

(comment
  (update-pomodoro-widget {:label "label"
                           :value "value"}))

(defn update-pomodoro-widget-handler
  ([] (update-pomodoro-widget-handler nil nil))
  ([_config parsed]
   (some-> parsed
           :arguments
           first
           update-pomodoro-widget)))

(defcom update-pomodoro-widget-cmd
  {:name          "update-pomodoro-widget"
   :one-line-desc "Updates the minutes passed on the pomodoro-widget"
   :description   [""]
   :handler       update-pomodoro-widget-handler})
