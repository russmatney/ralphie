(ns ralphie.garden
  (:require
   [ralphie.command :refer [defcom]]
   [ralphie.config :as config]
   [clojure.string :as string]
   [ralphie.sh :as sh]
   [org-crud.headline :as headline]
   [org-crud.core :as org-crud]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; garden->markdown
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn item->frontmatter [item]
  (let [name (:name item)]
    ["---" (str "title: " name) "---"]))

(defn item->content [item]
  (->> item :body (map :text) (remove nil?) (remove empty?) seq))

(defn item->md-lines [item]
  (concat
    (item->frontmatter item)
    (item->content item)))

(defn roam-md-files []
  (->> (config/roam-dir)
       org-crud/dir->nested-items
       (map (fn [item]
              {:filename (str (:name item) ".md")
               :lines    (item->md-lines item)}))))

(comment
  (def --items
    (->> (config/roam-dir)
         org-crud/dir->nested-items))

  (def --item
    (->> --items
         first))

  (-> --item
      ((fn [it]
         (let [name (:name it)]
           (concat
             ["---" (str "title: " name) "---"])
           ))))
  )


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Export garden
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn export-garden [_config _parsed]
  (let [md-files   (roam-md-files)
        target-dir (sh/expand "~/tempdir")]
    (for [file md-files]
      (spit (str target-dir "/" (:filename file))
            (->> file :lines (string/join "\n"))))))

(defcom export-garden-command
  {:name          "export-garden"
   :one-line-desc "Exports the org-roam dir as a blog."
   :description   ["Parses the org-roam dir into an intermediary."
                   "Writes to an out dir markdown."
                   "Handles backlinks."]
   :handler       export-garden})
