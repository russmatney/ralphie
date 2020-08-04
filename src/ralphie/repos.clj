(ns ralphie.repos
  (:require
   [ralphie.command :refer [defcom]]
   [ralphie.item :as item]
   [ralphie.config :as config]
   [ralphie.awesome :as awm]
   [org-crud.core :as org-crud]))

(defn parse-repos []
  (->> (config/repos-file)
       (org-crud/path->flattened-items)
       (filter (comp #(= % 1) :level))))

(defn dirty-repos []
  (->> (parse-repos)
       (filter item/watching?)
       ;; TODO re-impl dirty?
       ;; (filter item/dirty?)
       ))

(comment
  (count (dirty-repos)))

(defn update-repos-handler
  ([] (update-repos-handler nil nil))
  ([_config _parsed]
   (-> (dirty-repos)
       ;; awm/update-dirty-repos-widget
       )))

(defcom update-repos-cmd
  {:name          "update-repos"
   :one-line-desc "Updates the dirty repos used by misc widgets."
   :handler       update-repos-handler})
