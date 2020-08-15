(ns ralphie.pomodoro
  (:require
   [ralphie.command :refer [defcom]]
   [ralphie.awesome :as awm]
   ;; [ralphie.notify :as notify]
   [ralphie.config :as config]
   [org-crud.api :as org-crud]))

(defn update-pomodoro-widget [msg]
  ;; (notify/notify {:subject "Updating pomodoro widget" :body msg})
  (->> msg
       (awm/awm-fn "update_pomodoro_widget")
       awm/awm-cli))

(comment
  (update-pomodoro-widget {:label "label"
                           :value "value"}))

(defn touch-pomodoros
  "Hack to trigger yodo's pomos db to update, which fires
  `update-pomodoro-widget` for us.

  Yikes, amirite?"
  []
  (->> (config/pomodoros-file)
       org-crud/path->flattened-items
       (remove (comp #(= % :root) :level))
       first
       ((fn [p]
          (org-crud/update!
            (:source-file p)
            p {:props {:touched (rand-int 500)}})))))

(comment
  (rand-int 130)
  (touch-pomodoros)
  (->> (config/pomodoros-file)
       org-crud/path->flattened-items
       (remove (comp #(= % :root) :level))
       first
       ((fn [p]
          (org-crud/update!
            (:source-file p)
            p {:props {:touched (shuffle ["sup" "boi"])}})))))

(defcom update-pomodoro-widget-cmd
  {:name          "update-pomodoro-widget"
   :one-line-desc "Updates the minutes passed on the pomodoro-widget"
   :description   [""]
   :handler       (fn [_ _] (touch-pomodoros))})
