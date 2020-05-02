(ns ralphie.i3
  (:require
   [clojure.string :as string]
   [cheshire.core :as json]
   [ralphie.config :as config]
   [clojure.java.shell :as sh]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; utils
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn msg [& args]
  ;; TODO maybe this goes to an i3 workspace
  ;; if it does, make sure the focus "stays" with the caller
  (apply sh/sh "i3-msg" args))

(defn tree! []
  (-> (msg "-t" "get_tree")
      :out
      (json/parse-string true)))

(defn workspaces []
  (-> (msg "-t" "get_workspaces")
      :out
      (json/parse-string true)))

(defn monitor-node
  []
  (let [monitor (config/monitor)]
    (some->> (tree!)
             :nodes
             (filter #(= (:name %) monitor))
             first)))

(defn content-node [m-node]
  (some->> m-node
           :nodes
           (filter #(= (:name %) "content"))
           first))

(defn workspace-for-name
  "Returns a workspace from tree for the passed workspace name.
  TODO: handle multiple monitors
  "
  [wsp-name]
  (some->> (monitor-node)
           content-node
           :nodes
           (filter #(string/includes? (:name %) wsp-name))
           first))

(comment
  (workspace-for-name "yodo"))

(defn ->nodes
  "Joins and flattens `:nodes` and `:floating_nodes` in x"
  [x]
  (flatten ((juxt :nodes :floating_nodes) x)))

(defn all-nodes []
  (->> (tree!)
       (tree-seq ->nodes ->nodes)))

(comment
  (def --t (tree!))

  ()
  )

(defn focused-workspace []
  (->> (tree!)
       ()
       ))

(defn focused-node
  "Returns a map describing the currently focused app."
  []
  (->> (all-nodes)
       (filter :focused)
       (first)))

(defn focused-app
  [] (-> (focused-node)
         :window_properties
         :class))

(defn current-workspace
  []
  (->>
    (workspaces)
    (filter :focused)
    first))

(defn workspace-name
  "Returns a simple workspace name for the focused workspace."
  []
  (-> (current-workspace)
      :name
      (#(string/split % #":"))
      second
      string/trim))

(defn workspace-number
  []
  (-> (current-workspace)
      :name
      (#(string/split % #":"))
      first))

(defn workspace-open?
  [name]
  (seq (workspace-for-name name)))

(comment
  (workspace-open? "dotfiles")
  (workspace-open? "gibber")
  )

(defn ->apps [workspace]
  workspace
  )

(comment
  (->apps (current-workspace))
  )

(defn apps-open?
  "Only searches within the passed workspace."
  [workspace apps]
  (->> (workspace-for-name workspace)
       (map ()
            )))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; i3 Workspace Upsert
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn upsert [{:keys [name]}]
  (let [current-number (workspace-number)
        new-name       (str current-number ":" name)]
    (println "renaming workspace to" new-name)
    ;; (i3-msg "rename" "workspace" "to" name)
    ))
