(ns ralphie.item
  (:require
   [org-crud.core :as org-crud]
   [ralphie.config :as config]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Helpers
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn has-tag? [item tag]
  (-> item :org/tags set (contains? tag)))

(defn has-status? [item status]
  (-> item :org/status (= status)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Public
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn done? [item]
  (has-status? item :status/done))

(defn cancelled? [item]
  (has-tag? item "cancelled"))

(defn focused-at [item]
  (-> item :org.prop/focused-at))

(defn awesome-tag-parent? [item]
  (-> item :org.prop/child-tag (= "awesometag")))

(defn watching? [item]
  (-> item :org.prop/watching))

(defn workspace-key [item] (some-> item :org.prop/workspace-key
                                   Integer/parseInt))

(comment
  (workspace-key {:org.prop/workspace-key "0 "}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Parse Helper
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn ->level-1-list [root-item pred]
  (some->> root-item
           :org/items
           (filter pred)
           first
           :org/items))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Item data
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn path [item] (-> item :org.prop/path))

;; TODO namespace the rofi fields (:rofi/label, :rofi/on-select)
(defn ->rofi-item
  "Preps an item for rofi display.

  (rofi/rofi {:msg \"Items list\"} (->> items (map ->rofi-item)))
  "
  [{:keys [name tags] :as item}]
  (-> item
      (assoc :label
             (str name
                  (when (seq tags)
                    (str " <span color='gray'>" tags "</span>"))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Dates
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn latest-date
  [item]
  (some-> item
          ((fn [ps]
             (or
               (:org.prop/created-at ps)
               (:org.prop/updated-at ps)
               (:org.prop/started-at ps)
               (:org.prop/finished-at ps)
               (:org.prop/seen-at ps))))))

(comment
  (->>
    (reduce
      (fn [items path]
        (apply conj
               items
               (org-crud/path->flattened-items path)))
      (list)
      [
       ;; (config/repos-file)
       ;; (config/projects-file)
       ;; (config/goals-file)
       (config/journal-file)
       ])
    first
    )

  (latest-date {:org.prop/created-at "hi"})
  )

