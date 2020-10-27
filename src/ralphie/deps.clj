(ns ralphie.deps
  (:require
   [ralphie.config :as config]
   [ralphie.outdated :as outdated]
   [ralphie.fs :as fs]
   [ralphie.command :refer [defcom]]
   [org-crud.core :as org-crud]))

(defn has-deps-edn? [repo]
  (-> repo
      :org/name
      (#(str (config/home-dir) "/" % "/deps.edn"))
      fs/exists?
      ))

(comment
  (has-deps-edn? {:org/name "russmatney/yodo"}))

(defn watched-repos []
  (->>
    (config/repos-file)
    org-crud/path->nested-item
    :org/items
    (map #(dissoc % :org/items :org/tags :org/status :org/id :org/raw-headline
                  :org/source-file :org/body :org/level))
    (filter #(-> % :org.prop/watching))))

(defn any-outdated-handler [_ _]
  (doall
    (->> (watched-repos)
         (filter has-deps-edn?)
         (map outdated/check-deps-for-repo))))

(defcom any-outdated
  {:name          "any-outdated"
   :one-line-desc "Checks if watched repos have any outdated deps."
   :handler       any-outdated-handler})

(comment)
