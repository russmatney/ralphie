(ns ralphie.prompts
  (:require
   [ralphie.config :as config]
   [ralphie.item :as item]
   [ralphie.rofi :as rofi]
   [ralphie.command :refer [defcom]]
   [org-crud.create :as org-crud.create]
   [org-crud.core :as org-crud]))

(defn items
  []
  (->> (config/prompts-file)
       org-crud/path->nested-item
       :items))

(comment
  (->> (items)
       first)
  (count (items)))

(defn new-prompt-handler
  ([] (new-prompt-handler nil nil))
  ([_config _parsed]
   (->> (items)
        (map item/->rofi-item)
        (rofi/rofi {:msg "Choose a prompt!"})
        ((fn [selection]
           (when selection
             (org-crud.create/create-roam-file
               (config/notes-dir) selection)))))))

(defcom new-prompt-cmd
  {:name          "new-prompt"
   :one-line-desc "Select prompts, creates a new file."
   :description   ["Pulls prompts from prompts.org, creates files in roam-dir."]
   :handler       new-prompt-handler})
