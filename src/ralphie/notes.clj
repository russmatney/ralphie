(ns ralphie.notes
  (:require
   [ralphie.command :refer [defcom]]
   [ralphie.config :as config]
   [ralphie.fs :as fs]
   [org-crud.markdown :as org.markdown]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Export notes
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn export-notes
  "Deletes the blog-notes-dir, recreates it, parses and copies the notes in.

  TODO maybe backup the files first? Recover in case of parse/write error?"
  [_config _parsed]
  (let [dest-dir (config/blog-notes-dir)
        src-dir  (config/notes-dir)]
    (fs/delete-dir dest-dir)
    (fs/mkdir dest-dir)
    (org.markdown/org-dir->md-dir src-dir dest-dir)))

(defcom export-notes-command
  {:name          "export-notes"
   :one-line-desc "Exports an org notes dir as markdown with backlinks."
   :description   ["Parses an org notes dir into `items`."
                   "Writes those items to an out dir as markdown."
                   "Adds handles backlinks to all notes."]
   :handler       export-notes})
