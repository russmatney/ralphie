(ns ralphie.focus
  (:require
   [ralphie.command :refer [defcom]]
   [ralphie.rofi :as rofi]
   [ralphie.item :as item]
   [ralphie.config :as config]
   [org-crud.core :as org-crud]
   [cheshire.core :as json]))

(defn parse-current-todos []
  (reduce (fn [todos path]
            (->> path
                 (org-crud/path->flattened-items)
                 (filter (comp #(= % 2) :level))
                 (remove item/done?)
                 (remove item/cancelled?)
                 (concat todos)))
          (list)
          [(config/repos-file)
           (config/projects-file)
           (config/goals-file)]))

(defn focuses []
  (->> (parse-current-todos)
       (filter item/focused-at)))

(comment
  (count (focuses)))

(defn focus-handler
  ([] (focus-handler nil nil))
  ([_config parsed]
   (cond
     (some-> parsed :arguments first (= "update-json"))
     (->> (focuses)
          first
          ((fn [focus]
             {:latest_focus focus}))
          cheshire.core/encode
          (spit (config/focus-file)))

     :else
     (->> (focuses)
          (map (fn [it]
                 (assoc it :label (:name it))))
          (rofi/rofi {:msg "Focuses"})))))

(defcom focus-cmd
  {:name          "focus"
   :one-line-desc "Returns your current focused todos"
   :description   ["Supports a json parameter: `ralphie focus json`."]
   :handler       focus-handler})
