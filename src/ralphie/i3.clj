(ns ralphie.i3
  (:require
   [clojure.string :as string]
   [cheshire.core :as json]
   [ralphie.config :as config]
   [ralphie.rofi :as rofi]
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

(defn workspaces []
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

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; current workspace
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn current-workspace
  []
  (->> (workspaces)
       (filter :focused)
       first))

(defn workspace-name
  "Returns a simple workspace name for the focused workspace."
  []
  (-> (current-workspace)
      :name
      (string/split #":")
      second
      string/trim))

(defn workspace-number
  []
  (-> (current-workspace)
      :name
      (string/split #":")
      first))

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
  (app-names-in-wsp "read")
  (apps-open? "read" ["Alacritty"]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; i3 Workspace Update
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn workspace->rofi-x [workspace]
  {:label (:name workspace)}
  )

(defn upsert
  "TODO Perhaps this logic should be in workspaces?"
  [{:keys [name]}]
  (let [name-to-update (->>
                         (workspaces)
                         (map workspace->rofi-x)
                         (rofi/rofi {:msg "Workspace to update?"})
                         :label)

        number   (some-> name-to-update (string/split #":") first)
        new-name (str number ": " name)]
    (println "renaming workspace from" name-to-update "to" new-name)
    (i3-msg! "rename" "workspace" "to" name)
    ))

(comment
  (upsert {:name "timeline"})
  )
