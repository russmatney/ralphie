(ns ralphie.i3
  (:require
   [clojure.string :as string]
   [cheshire.core :as json]
   [ralphie.config :as config]
   [ralphie.rofi :as rofi]
   [ralphie.command :refer [defcom]]
   [clojure.java.shell :as sh]
   [clojure.set :as set]))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; i3-msg
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn i3-msg! [& args]
  (apply sh/sh "i3-msg" args))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; i3-data roots
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn tree []
  (-> (i3-msg! "-t" "get_tree")
      :out
      (json/parse-string true)))

(defn workspaces-simple []
  (-> (i3-msg! "-t" "get_workspaces")
      :out
      (json/parse-string true)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; mid-parse utils
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn monitor-node
  []
  (let [monitor (config/monitor)]
    (some->> (tree)
             :nodes
             (filter #(= (:name %) monitor))
             first)))

(defn content-node [m-node]
  (some->> m-node
           :nodes
           (filter #(= (:name %) "content"))
           first))

(defn flatten-nodes
  "Joins and flattens `:nodes` and `:floating_nodes` in x"
  [x]
  (flatten ((juxt :nodes :floating_nodes) x)))

(defn tree->nodes [tr]
  (tree-seq flatten-nodes flatten-nodes tr))

(defn all-nodes []
  (->> (tree)
       tree->nodes))

(defn workspaces-from-tree []
  (->> (monitor-node)
       content-node
       :nodes))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; current workspace
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn current-workspace
  "TODO rewrite to use get_tree"
  []
  (some->> (workspaces-simple)
           (filter :focused)
           first
           :name
           ((fn [name]
              (some->> (workspaces-from-tree)
                       (filter (fn [node]
                                 (= (:name node) name)))
                       first)))))

(comment
  (println "\n\n\nbreak\n\n\n")
  ;; (clojure.pprint/pprint
  ;;   (current-workspace))
  )

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; focused node/apps
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn focused-node
  "Returns a map describing the currently focused app."
  []
  (->> (all-nodes)
       (filter :focused)
       first))

(defn focused-app
  [] (-> (focused-node)
         :window_properties
         :class))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; workspace for name
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn workspace-for-name
  "Returns a workspace from tree for the passed workspace name.
  TODO: handle multiple monitors
  "
  [wsp-name]
  (some->> (monitor-node)
           content-node
           :nodes
           (filter #(string/includes? (:name %) wsp-name))
           first
           ))

(defn nodes-for-wsp-name
  [name]
  (-> name
      workspace-for-name
      tree->nodes))

(defn app-names-in-wsp
  [name]
  (->> name
       nodes-for-wsp-name
       (map (comp :class :window_properties))))

(defn workspace-open?
  [name]
  (seq (workspace-for-name name)))

(defn apps-open?
  "Only searches within the passed workspace."
  [workspace apps]
  (-> workspace
      app-names-in-wsp
      set
      ((fn [open-apps]
         (set/subset? (set apps) open-apps)))))

(comment
  (workspace-for-name "yodo")
  (app-names-in-wsp "read")
  (apps-open? "read" ["Alacritty"]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; i3 Workspace Upsert
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn visit-workspace [number]
  (i3-msg! "workspace" "number" number))

(defn rename-workspace [name number]
  []
  (i3-msg! "rename" "workspace" "to" (str number ":" name)))

(defn upsert
  "TODO Perhaps this logic should be in workspaces?"
  [{:keys [name]}]
  (let [name-to-update   (->> (workspaces-simple)
                              (map :name)
                              (rofi/rofi {:msg "Workspace to update?"}))
        number-to-update (some-> name-to-update (string/split #":") first)]
    (rename-workspace name number-to-update)))

(comment
  (upsert {:name "timeline"})
  )

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; workspaces.org items -> i3 config
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn workspace->i3-config-lines
  [_workspace]
  ;; TODO convert parsed workspace to i3 config
  [])

(defn write-i3-ralphie
  "Parses misc org data into an i3 config. Writes to the passed file."
  [file]
  (let [
        ;; TODO parse workspaces from org (requires yodo org parser)
        workspaces []]
    (->> workspaces
         (map workspace->i3-config-lines)
         (apply concat)
         (string/join "\n")
         (spit file))))

(defn rebuild-i3-config!
  "The i3 config is partially based on data in <org-dir>/workspaces.org.
  This function converts that data to <i3-config-dir>/config.ralphie and
  concatenates it with <i3-config-dir>/config.base.
  "
  []
  (let [i3-config-file (str (config/i3-dir) "/config")
        base-file      (str (config/i3-dir) "/config.base")
        ralphie-file   (str (config/i3-dir) "/config.ralphie")]
    (write-i3-ralphie ralphie-file)
    (sh/sh "rm" i3-config-file)
    (sh/sh "cat" base-file ">>" i3-config-file)
    (sh/sh "cat" ralphie-file ">>" i3-config-file)))

(defn restart-i3! [] (i3-msg! "restart"))

(defn rebuild-and-restart!
  "Converts passed workspaces into an i3 config.
  The contents is written to i3/config.ralphie,
  which is then concattenated with i3.config.base."
  [_config _parsed]
  ;; TODO try-catch the rebuild, always restart
  (rebuild-i3-config!)
  (restart-i3!))

(defcom rebuild-and-restart-i3
  {:name          "restart-i3"
   :one-line-desc "Restarts i3 in place"
   :handler       rebuild-and-restart!})
