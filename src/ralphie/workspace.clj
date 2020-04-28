(ns ralphie.workspace
  (:require [ralphie.i3 :as i3]))

(defn upsert-cmd
  "Updates the current workspace."
  [_config parsed]
  ;; TODO parse arguments intelligently
  (let [name (first (:arguments parsed))]
    (i3/upsert {:name name})))

(def upsert
  {:name          "workspace-upsert"
   :one-line-desc "Updates a workspace to match the passed data"
   :description   ["Supports :name"]
   :handler       upsert-cmd})

(defn current
  "Returns the current workspace."
  []
  (let [name (i3/workspace-name)]
    {:name name}))
