(ns ralphie.item)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Helpers
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn has-tag? [item tag]
  (-> item :org/tags set (contains? tag)))

(defn has-status? [item status]
  (-> item :org/status #{status} some?))

(comment
  (has-status? {:org/status :status/hi} :status/bye))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Public
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn awesome-name [item]
  (-> item :awesome/name))

(defn awesome-index [item]
  (-> item :awesome/index))

(defn awesome-selected [item]
  (-> item :awesome/selected))

(defn awesome-empty [item]
  (-> item :awesome/empty))


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

(defn label [item] (-> item :org/name))

;; TODO namespace the rofi fields (:rofi/label, :rofi/on-select)
(defn ->rofi-item
  "Preps an item for rofi display.

  (rofi/rofi {:msg \"Items list\"} (->> items (map ->rofi-item)))
  "
  [{:keys [org/tags] :as item}]
  (-> item
      (assoc :label
             (str (label item)
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
