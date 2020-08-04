(ns ralphie.focus
  (:require
   [ralphie.command :refer [defcom]]
   [ralphie.rofi :as rofi]
   [ralphie.item :as item]
   [ralphie.config :as config]
   [ralphie.awesome :as awm]
   [org-crud.core :as org-crud]))

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
       (filter item/focused-at)
       ;; not yet a true date comparison
       (sort-by (comp :focused-at :props))
       (reverse)))

(comment
  (count (focuses)))

(defn set-focus-handler
  ([] (set-focus-handler nil nil))
  ([_config parsed]
   (cond
     (some-> parsed :arguments first (= "first"))
     ;; used to let the awesome focus widget request an update
     (-> (focuses)
         first
         awm/update-focus-widget)

     :else
     ;; interactive selection of current focus
     (->> (focuses)
          (map (fn [it]
                 (assoc it :label (:name it))))
          (rofi/rofi {:msg "Focuses"})
          ;; TODO write 'focused-at' date-time back to org
          awm/update-focus-widget))))

(defcom set-focus-cmd
  {:name          "set-focus"
   :one-line-desc "Returns your current focused todos"
   :description   ["Supports a json parameter: `ralphie focus json`."]
   :handler       set-focus-handler})
