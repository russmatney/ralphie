(ns ralphie.workspace
  (:require
   [ralphie.awesome :as awm]
   [ralphie.rofi :as rofi]
   [ralphie.command :refer [defcom]]
   [ralphie.config :as config]
   [ralphie.notify :refer [notify]]
   [org-crud.api :as org-crud]
   [clojure.string :as string]))

;; (defn rename-handler
;;   "Updates a selected workspace with the passed name."
;;   [_config parsed]
;;   (let [name (or (some-> parsed :arguments first)
;;                  (rofi/rofi {:msg "New name for workspace"}))]
;;     (i3/upsert {:name name})))

;; (defcom rename-cmd
;;   {:name          "rename-workspace"
;;    :one-line-desc "Updates a workspace to match the passed data"
;;    :description   ["Supports name as the first argument."]
;;    :handler       rename-handler})

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
             (merge org-wsp
                    {:awesome/tag (awm/tag-for-name name awm-all-tags)}))))))

(comment
  (->>
    (all-workspaces)
    (filter :awesome/tag)))

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

;; (defn start-workspace-handler [_config _parsed]
;;   (let [selected         (->> (all-workspaces)
;;                               (map (fn [wsp]
;;                                      (assoc wsp :label
;;                                             (-> wsp :org/item :name))))
;;                               (rofi/rofi {:msg "Select workspace"}))
;;         name             (-> selected :org/item :name)
;;         workspace-number (rofi/rofi
;;                            {:msg "Take over workspace number:"}
;;                            [1 2 3 4 5 6 7 8 9 0])]
;;     (i3/visit-workspace workspace-number)
;;     (i3/rename-workspace name workspace-number)))


;; (defcom start-workspace-cmd
;;   {:name          "start-workspace"
;;    :one-line-desc "start-workspace"
;;    :description
;;    ["Creates a new workspace based on workspaces.org and rofi input."]
;;    :handler       start-workspace-handler})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; New create workspace
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn awesome-create-tag-handler
  [_ {:keys [arguments]}]
  (if-let [tag-name (some-> arguments first)]
    (do
      (notify (str "Found workspace, creating: " tag-name))
      (awm/create-tag! tag-name))

    ;; no tag, get from rofi
    (let [existing-tag-names (->> (awm/all-tags) (map :name) set)
          selected-wsp
          (rofi/rofi
            {:msg "New Tag Name?"}
            (->>
              (all-workspaces)
              (map :org/name)
              (remove #(contains? existing-tag-names %))
              seq))]
      (-> selected-wsp awm/create-tag!)
      (notify (str "Created new workspace: " selected-wsp)))))

(defcom awesome-create-tag
  {:name          "create-workspace"
   :one-line-desc "Creates a new tag in your _Awesome_ Window Manager."
   :description   []
   :handler       awesome-create-tag-handler})

(comment)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; consolidate workspaces
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn consolidate-workspaces []
  (->>
    (all-workspaces)
    (filter :awesome/tag)
    (filter (comp #(> % 0) count :clients :awesome/tag))
    (sort-by (comp :index :awesome/tag))
    (map-indexed
      (fn [new-index {:keys [:awesome/tag]}]
        (let [new-index            (+ 1 new-index) ;; b/c lua 1-based
              {:keys [name index]} tag]
          (if (== index new-index)
            (prn "nothing to do")
            (do
              (prn "swapping tags" {:name      name
                                    :idx       index
                                    :new-index new-index})
              (awm/awm-cli
                (str "local tag = awful.tag.find_by_name(nil, \"" name "\");"
                     "local tags = awful.screen.focused().tags;"
                     "local tag2 = tags[" new-index "];"
                     "tag:swap(tag2);")))))))))

(comment
  (consolidate-workspaces))

(defn consolidate-workspaces-handler
  ([] (consolidate-workspaces-handler nil nil))
  ([_config _parsed]
   (consolidate-workspaces)))

(defcom consolidate-workspaces-cmd
  {:name          "consolidate-workspaces"
   :one-line-desc "Groups active workspaces closer together"
   :description   ["Moves active workspaces to the front of the list."]
   :handler       consolidate-workspaces-handler})
