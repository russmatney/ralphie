(ns ralphie.workspace
  (:require
   [ralphie.i3 :as i3]
   [ralphie.rofi :as rofi]))

(defn rename-handler
  "Updates a selected workspace with the passed name."
  [_config parsed]
  (let [name (or (some-> parsed :arguments first)
                 (rofi/rofi {:msg "New name for workspace"}))]
    (i3/upsert {:name name})))

(def rename-cmd
  {:name          "rename-workspace"
   :one-line-desc "Updates a workspace to match the passed data"
   :description   ["Supports name as the first argument."]
   :handler       rename-handler})

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

(comment
  (open? {:app :term}))
