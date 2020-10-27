(ns ralphie.workspace
  (:require
   [ralphie.i3 :as i3]
   [ralphie.awesome :as awm]
   [ralphie.rofi :as rofi]
   [ralphie.command :refer [defcom]]
   [ralphie.config :as config]
   [org-crud.api :as org-crud]
   [clojure.string :as string]))

(defn rename-handler
  "Updates a selected workspace with the passed name."
  [_config parsed]
  (let [name (or (some-> parsed :arguments first)
                 (rofi/rofi {:msg "New name for workspace"}))]
    (i3/upsert {:name name})))

(defcom rename-cmd
  {:name          "rename-workspace"
   :one-line-desc "Updates a workspace to match the passed data"
   :description   ["Supports name as the first argument."]
   :handler       rename-handler})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; org-workspaces
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn all-workspaces []
  (let [awm-all-tags (awm/all-tags)]
    (->>
      "workspaces.org"
      (#(str (config/org-dir) "/" %))
      (org-crud/path->flattened-items)
      (map (fn [{:keys [org/name] :as org-wsp}]
             ;; TODO move to namespaced fields
             (merge org-wsp
                    {:awesome/tag (awm/tag-for-name name awm-all-tags)
                     ;; :i3/workspace (i3/workspace-for-name name)
                     }))))))

(defn for-name [name]
  (some->> (all-workspaces)
           (filter
             #(some->>
                % :org/name
                (string/includes? name)))
           first))

(comment
  (for-name "yodo-dev"))

(defn current-workspace
  []
  (for-name (awm/current-tag-name)))

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


(defcom start-workspace-cmd
  {:name          "start-workspace"
   :one-line-desc "start-workspace"
   :description
   ["Creates a new workspace based on workspaces.org and rofi input."]
   :handler       start-workspace-handler})

(defn awesome-create-tag-handler
  [_ {:keys [arguments]}]
  (println "arguments" arguments)
  (if-let [tag-name (some-> arguments first)]

    (do
      (println "found args, tag-name" tag-name)
      (awm/create-tag! tag-name))

    ;; no tag, get from rofi
    (let [existing-tag-names (->> (awm/all-tags) (map :name) set)]
      (println "starting rofi")
      (println "existing" existing-tag-names)
      (rofi/rofi
        {:msg       "New Tag Name?"
         :on-select awm/create-tag!}
        (->>
          (all-workspaces)
          (map :org/name)
          (remove #(contains? existing-tag-names %))
          seq)))) )

(defcom awesome-create-tag
  {:name          "awesome-create-tag"
   :one-line-desc "Creates a new tag in your _Awesome_ Window Manager."
   :description   []
   :handler       awesome-create-tag-handler})

(comment)
