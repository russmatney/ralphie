(ns ralphie.garden
  (:require
   [ralphie.command :refer [defcom]]
   [ralphie.config :as config]
   [org-crud.markdown :as org.markdown]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Export garden
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn export-garden [_config _parsed]
  (org.markdown/org-dir->md-dir
    (config/roam-dir)
    (config/blog-garden-dir)))

(defcom export-garden-command
  {:name          "export-garden"
   :one-line-desc "Exports the org-roam dir as a blog."
   :description   ["Parses the org-roam dir into an intermediary."
                   "Writes to an out dir markdown."
                   "Handles backlinks."]
   :handler       export-garden})
