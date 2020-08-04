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

(defn path [item] (-> item :props :path))
