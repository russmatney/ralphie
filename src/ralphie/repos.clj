(ns ralphie.repos
  (:require
   [ralphie.command :refer [defcom]]
   [ralphie.item :as item]
   [ralphie.config :as config]
   [ralphie.awesome :as awm]
   [ralphie.fs :as fs]
   [ralphie.rofi :as rofi]
   [org-crud.core :as org-crud]
   [clojure.string :as string]
   [clojure.java.shell :as sh]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Repo helpers
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn ignore-dirty? [item]
  (-> item item/path (string/includes? "Dropbox")))

(defn is-clean? [item]
  (if (ignore-dirty? item)
    true
    (let [{:keys [out exit err]}
          (sh/sh "git" "diff" "HEAD" :dir (item/path item))]
      (and
        (= out "")
        (= err "")
        (= exit 0)))))

(comment
  (is-clean? {:props {:path "/home/russ/Dropbox/todo"}})
  (is-clean? {:props {:path "/home/russ/russmatney/ralphie"}})
  )

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Fetch repos from org file
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn fetch-repos []
  (->> (config/repos-file)
       (org-crud/path->flattened-items)
       (filter (comp #(= % 1) :org/level))
       (map (fn [repo]
              (let [path (str (config/home-dir) "/" (:org/name repo))]
                (-> repo
                    (assoc :org.prop/path path)))))
       (filter (comp fs/exists? item/path))))

(defn dirty-repos []
  (->> (fetch-repos)
       (filter item/watching?)
       (map (fn [repo]
              (-> repo
                  (assoc :clean? (is-clean? repo)))))
       (remove :clean?)))

(comment
  (->> (dirty-repos)
       (map :name)
       (println))
  (count (dirty-repos)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Update repos widget
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn update-repos-handler
  ([] (update-repos-handler nil nil))
  ([_config _parsed]
   (->> (dirty-repos)
        (awm/awm-fn "update_repos_widget")
        awm/awm-cli)))

(defcom update-repos-cmd
  {:name          "update-dirty-repos"
   :one-line-desc "Updates the dirty repos used by misc widgets."
   :handler       update-repos-handler})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; List dirty repos
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn list-dirty-repos-handler
  ([] (list-dirty-repos-handler nil nil))
  ([_config _parsed]
   (->> (dirty-repos)
        (map (fn [x] (assoc x :label (:name x))))
        (rofi/rofi {:msg "Dirty Repos"})
        )))

(defcom list-dirty-repos-cmd
  {:name          "list-dirty-repos"
   :one-line-desc "Updates the dirty repos used by misc widgets."
   :handler       list-dirty-repos-handler})

(comment)
