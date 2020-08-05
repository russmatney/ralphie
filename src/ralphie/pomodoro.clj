(ns ralphie.pomodoro
  (:require
   [ralphie.command :refer [defcom]]
   [ralphie.rofi :as rofi]
   [ralphie.item :as item]
   [ralphie.config :as config]
   [ralphie.awesome :as awm]
   [ralphie.items :as items]
   [org-crud.core :as org-crud]))

(defn pomodoro-minutes []
  ;; TODO find minutes since first timestamp after last 15+ minute gap
  (->>
    (items/recent-items)
    )
  12)

(comment
  (pomodoro-minutes))

(defn update-pomodoro-widget-handler
  ([] (update-pomodoro-widget-handler nil nil))
  ([_config _parsed]
   (->> (pomodoro-minutes)
        ((fn [mins] (str mins " mins")))
        (awm/awm-fn "update_pomodoro_widget")
        awm/awm-cli)
   ))

(defcom update-pomodoro-widget-cmd
  {:name          "update-pomodoro-widget"
   :one-line-desc "Updates the minutes passed on the pomodoro-widget"
   :description   [""]
   :handler       update-pomodoro-widget-handler})
