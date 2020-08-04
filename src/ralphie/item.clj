(ns ralphie.item)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Helpers
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn has-tag? [item tag]
  (-> item :tags set (contains? tag)))

(defn has-status? [item status]
  (-> item :status (= status)))

(defn has-prop?
  ([item prop]
   (-> item :props (get prop)))

  ([item prop value]
   (-> item :props (get prop) (= value))
   ))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Public
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn done? [item]
  (has-status? item :status/done))

(defn cancelled? [item]
  (has-tag? item "cancelled"))

(defn focused-at [item]
  (has-prop? item :focused-at))

(defn awesome-tag-parent? [item]
  (has-prop? item :child-tag "awesometag"))

(defn watching? [item]
  (has-prop? item :watching))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Parse Helper
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn ->level-1-list [root-item pred]
  (some->> root-item
           :items
           (filter pred)
           first
           :items))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Item data
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def props :props)

(defn path [item] (-> item :props :path))

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
  (some-> item :props
          ((fn [ps]
             (or
               (:created-at ps)
               (:updated-at ps)
               (:started-at ps)
               (:finished-at ps)
               (:seen-at ps))))))

(comment
  (latest-date {:props {:created-at "hi"}})
  )

