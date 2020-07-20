(ns ralphie.deps
  (:require
   [ralphie.config :as config]
   [ralphie.outdated :as outdated]
   [ralphie.fs :as fs]
   [ralphie.command :refer [defcom]]
   [org-crud.core :as org-crud]))

(defn has-deps-edn? [repo]
  (-> repo
      :name
      (#(str (config/home-dir) "/" % "/deps.edn"))
      fs/exists?
      ))

(comment
  (has-deps-edn? {:name "russmatney/yodo"}))

(defn watched-repos []
  (->>
    (config/repos-file)
    org-crud/path->nested-item
    :items
    (map #(dissoc % :items :tags :status :id :raw-headline :source-file
                  :body :level))
    (filter #(-> % :props :watching))))

(defcom any-outdated
  {:name          "any-outdated"
   :one-line-desc "Checks if watched repos have any outdated deps."
   :handler
   (fn [_ _]
     (doall
       (->> (watched-repos)
            (filter has-deps-edn?)
            (map outdated/check-deps-for-repo))))})
