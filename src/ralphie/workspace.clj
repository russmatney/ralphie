(ns ralphie.workspace
  (:require
   [ralphie.i3 :as i3]
   [ralphie.org :as org]
   [ralphie.rofi :as rofi]
   [clojure.string :as string]))

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
;; org-workspaces
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn all-workspaces []
  (->>
    (org/fname->items "workspaces.org")
    (map (fn [{:keys [name] :as org-wsp}]
           {:org/item     org-wsp
            :i3/workspace (i3/workspace-for-name name)}))))

(defn full-workspace
  ([]
   (full-workspace nil))
  ([name]
   (let [name (or name (-> (i3/current-workspace) :name))]
     (->> (all-workspaces)
          (filter #(->> % :org/item :name (string/includes? name)))
          first))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; current workspace
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn ->workspace
  []
  (let [full (full-workspace)]
    (merge
      (-> full :org/item :props)
      {:name (-> full :org/item :name)})))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; restart workspaces in-place (i3)
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn restart-workspaces-handler [_config _parsed]
  (println "TODO restarting i3!")
  ;; (i3/org->i3! (all-workspaces))
  ;; (i3/i3-msg! "restart")
  )

(def restart-workspaces-cmd
  {:name          "restart-workspaces"
   :one-line-desc "restart-workspaces"
   :description   ["Restarts i3 in place."
                   "Builds an updated config based on workspaces.org"]
   :handler       restart-workspaces-handler})


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; start workspace
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn start-workspace-handler [_config _parsed]
  (let [selected         (->> (all-workspaces)
                              (map (fn [wsp]
                                     (assoc wsp :label
                                            (-> wsp :org/item :name))))
                              (rofi/rofi {:msg "Select workspace"}))
        name             (-> selected :org/item :name)
        workspace-number (rofi/rofi
                           {:msg "Take over workspace number:"}
                           [1 2 3 4 5 6 7 8 9 0])]
    (i3/visit-workspace workspace-number)
    (i3/rename-workspace name workspace-number)))


(def start-workspace-cmd
  {:name          "start-workspace"
   :one-line-desc "start-workspace"
   :description
   ["Creates a new workspace based on workspaces.org and rofi input."]
   :handler       start-workspace-handler})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; open? helper
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(defn open?
  "Returns true if the specified workspace or app is open in the
  current/specified workspace."
  [{:keys [app apps name]}]
  (if name
    (i3/workspace-open? name)
    (let [apps (or apps [app])
          name (or name (:name (->workspace)))]
      (i3/apps-open? name apps))))

(comment
  (open? {:app :term}))
