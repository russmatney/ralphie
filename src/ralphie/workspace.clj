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
   :description   ["Supports :name."
                   "Not yet implemented."]
   :handler       upsert-cmd})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; current workspace
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn current
  "Returns the current workspace."
  []
  (let [name (i3/workspace-name)]
    (println name)
    {:name name}))

(defn open?
  "Returns true if the specified workspace or app is open in the
  current/specified workspace."
  [{:keys [app apps name]}]
  (if name
    (i3/workspace-open? name)
    (let [apps (or apps [app])
          name (or name (:name (current)))]
      (i3/apps-open? name apps))))
