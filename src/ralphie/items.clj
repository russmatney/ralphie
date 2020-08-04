(ns ralphie.items
  (:require
   [org-crud.core :as org-crud]
   [ralphie.item :as item]
   [ralphie.rofi :as rofi]
   [ralphie.config :as config]
   [ralphie.command :refer [defcom]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Fetch items
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn items
  "Slow. Parses all the items."
  []
  (reduce
    (fn [items path]
      (apply conj
             items
             (org-crud/path->flattened-items path)))
    (list)
    (config/item-paths))
  )

(comment
  (->> (items)
       first)
  (count (items)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Helper
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn recent-items
  "Removes and then sorts by latest-date."
  []
  (->> (items)
       (filter item/latest-date)
       (sort-by item/latest-date)
       (reverse)
       (take 50)))

(defn list-items-handler
  "Parses items, maps to rofi, passes to rofi."
  ([] (list-items-handler nil nil))
  ([_config _parsed]
   (let [items (recent-items)]
     (->> items
          (map item/->rofi-item)
          (rofi/rofi {:msg "Recent Items"})))))

(defcom list-items-cmd
  {:name          "list-items"
   :one-line-desc "Lists items based on recency."
   :description   ["Recency using the lastest date available on the item."]
   :handler       list-items-handler})
