(ns ralphie.workspace
  (:require
   [ralphie.awesome :as awm]
   [ralphie.rofi :as rofi]
   [ralphie.repos :as repos]
   [ralph.defcom :refer [defcom]]
   [ralphie.config :as config]
   [ralphie.notify :refer [notify]]
   [org-crud.api :as org-crud]
   [clojure.string :as string]
   [ralphie.item :as item]
   [ralphie.git :as git]
   [ralphie.fs :as fs]))

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
      org-crud/path->nested-item
      :org/items
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
  "Returns the currently active workspace according to the current
  selected awesome tags. If multiple tags are found, the non-scratchpad
  workspace is preferred." []
  (let [tag-names (awm/current-tag-names)
        wkspcs    (map for-name tag-names)]
    (->> wkspcs
         ;; seems to sort scratchpads to the end... w/e!
         (sort-by item/scratchpad?)
         first)))

(comment
  (current-workspace))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; workspace repos
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn dirty-workspaces []
  (->> (all-workspaces)
       (filter :org.prop/directory)
       (filter (fn [x]
                 (let [path (-> x :org.prop/directory)]
                   (and
                     (fs/exists? path)
                     (fs/exists? (str path "/.git"))))))
       (remove (comp git/is-clean? :org.prop/directory))))

(defn list-dirty-workspaces-handler
  ([] (list-dirty-workspaces-handler nil nil))
  ([_config _parsed]
   (->> (dirty-workspaces)
        (map :org/name)
        (rofi/rofi {:msg "Dirty Workspaces"})
        ;; TODO write awm func that creates+focuses or focuses if already created
        awm/create-tag!
        awm/focus-tag!)
   ))

(defcom list-dirty-workspaces-cmd
  {:name          "list-dirty-workspaces"
   :one-line-desc "Lists workspaces with dirty git repos"
   :description   ["Selecting a workspace will open it up."]
   :handler       list-dirty-workspaces-handler})

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

(defn choose-workspace []
  (let [existing-tag-names (->> (awm/all-tags) (map :name) set)]
    (rofi/rofi
      {:msg "New Workspace Name?"}
      (->>
        (all-workspaces)
        (concat
          {:rofi/label     "Load repos?"
           :rofi/on-select (fn []
                             (rofi/rofi
                               {:msg "New Workspace from repo?"}
                               (repos/fetch-repos)))})
        (map :org/name)
        (remove #(contains? existing-tag-names %))
        seq))))

(comment
  ;; is `keep` just `(comp map filter)`
  (keep (fn [x] (when-not (= x 0) (- x 1)))
        [1 2 3 4])
  (choose-workspace))

(defn create-workspace-handler
  [_ {:keys [arguments]}]
  (if-let [tag-name (some-> arguments first)]
    (do
      (notify (str "Found workspace, creating: " tag-name))
      (awm/create-tag! tag-name))

    ;; no tag, get from rofi
    ;; TODO support selecting already open wrkspc and just focusing
    (some-> (choose-workspace)
            awm/create-tag!
            awm/focus-tag!
            ((fn [name]
               (notify (str "Created new workspace: " name)))))

    ))

;; TODO need tests on this!!!
(defcom create-workspace
  {:name          "create-workspace"
   :one-line-desc "Creates a new tag in your _Awesome_ Window Manager."
   :description   []
   :handler       create-workspace-handler})


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

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; clean up workspaces
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn clean-up-workspaces
  "Closes workspaces with 0 clients."
  []
  (->>
    (all-workspaces)
    (filter :awesome/tag)
    (filter (comp #(= % 0) count :clients :awesome/tag))
    (map
      (fn [{:keys [:awesome/tag]}]
        (let [{:keys [name]} tag]
          (awm/delete-tag! name))))))

(comment
  (clean-up-workspaces))

(defn clean-up-workspaces-handler
  ([] (clean-up-workspaces-handler nil nil))
  ([_config _parsed] (clean-up-workspaces)))

(defcom clean-up-workspaces-cmd
  {:name          "clean-up-workspaces"
   :one-line-desc "Closes workspaces that have no active clients"
   :description   []
   :handler       clean-up-workspaces-handler})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; swap workspaces indexes
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; (defn awm-cli-fnl [fnl args]
;;   (println "run this fnl plz" fnl)
;;   (println "with these args" args))

;; (defn swap-workspace
;;   "Swaps the current awesome workspace in the given direction"
;;   [dir]
;;   (let [up?   (= dir "up")
;;         down? (= dir "down")]
;;     (if (or up? down?)
;;       ;; TODO hook up clj kondo hook
;;       (awm-cli-fnl
;;         (let [tags          (. (awful.screen.focused) :tags)
;;               current-tag   (. (awful.screen.focused) :tag)
;;               current-index (. current-tag :index)
;;               new-index     (+ current-index %)
;;               new-tag       (. tags new-index)
;;               ]
;;           (if new-tag
;;             (:> current-tag :swap new-tag)))
;;         (cond up? 1 down? -1))
;;       (notify "swap-workspace called without 'up' or 'down'!")))
;;   )

(defn swap-workspace
  "Swaps the current awesome workspace in the given direction"
  [dir]
  (let [up?   (= dir "up")
        down? (= dir "down")]
    (if (or up? down?)
      (awm/awm-cli
        (str
          "tags = awful.screen.focused().tags; "
          "current_index = s.selected_tag.index; "
          "new_index = current_index " (cond up? "+ 1" down? "- 1" ) "; "
          "new_tag = tags[new_index]; "
          "if new_tag then s.selected_tag:swap(new_tag) end; "
          ))
      (notify "swap-workspace called without 'up' or 'down'!"))))

(comment
  (println "hi")
  (swap-workspace "up"))

(defn swap-workspace-index-handler
  ([] (swap-workspace-index-handler nil nil))
  ([_config {:keys [arguments]}]
   (let [[dir & _rest] arguments]
     (swap-workspace dir))))

(defcom swap-workspace-index-cmd
  {:name          "swap-workspace-index"
   ;; TODO *keys-pressed* as a dynamic var/macro or partially applied key in your keybinding
   ;; TODO support keybindings right here
   :keybinding    "ctrl-shift-p"
   :one-line-desc "Swaps a workspace up or down an index."
   :description   ["Intended to feel like dragging a workspace in a direction."]
   :handler       swap-workspace-index-handler})
