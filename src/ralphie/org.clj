(ns ralphie.org
  (:require
   [ralphie.config :as config]
   [organum.core :as org]
   [clojure.string :as string]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn ->body
  [{:keys [content]}]
  (let [body
        (->> content
             (partition-by #(= :blank (:line-type %)))
             (flatten))

        ;; drop blank first line
        first-line-blank?
        (-> body
            (first)
            :line-type
            (= :blank))]
    (if first-line-blank?
      (rest body)
      body)))

(defn ->drawer
  [{:keys [content]}]
  (->> content
       (filter (fn [c] (-> c :type (= :drawer))))
       first
       :content
       (map :text)))

(defn ->prop-key [text]
  (let [[key _val] (string/split text #" " 2)]
    (when key
      (-> key
          (string/replace ":" "")
          (string/replace "_" "-")
          (string/replace "+" "")
          string/lower-case
          keyword))))

(defn ->prop-value [text]
  (when (->prop-key text)
    (some-> text
            (string/split #" " 2)
            second
            string/trim)))

(defn ->properties [x]
  (let [drawer-items (->drawer x)]
    (if (seq drawer-items)
      (->> drawer-items
           (group-by ->prop-key)
           (map (fn [[k vals]]
                  [k (cond->> vals
                       true
                       (map ->prop-value)

                       (= (count vals) 1)
                       first)]))
           (into {}))
      {})))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn ->item [{:keys [name level] :as section}]
  {:level level
   :name  name
   :tags  (-> section :tags set)
   ;; :body  (->body section)
   :props (->properties section)})

(defn parsed->items
  "Parses :type :section (skipping :root) into org-items.
  Organum does not automatically nest org-sections, so items from multiple
  levels will be siblings rather than in a hierarchy.
  "
  [parsed]
  (reduce
    (fn [items next]
      (if (= :section (:type next))
        (conj items (->item next))
        items))
    []
    parsed))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn fname->items [fname]
  (-> (config/org-dir)
      (str "/" fname)
      org/parse-file
      parsed->items))

(comment
  (def --w
    (fname->items "workspaces.org")))