(ns ralphie.i3
  (:require
   [clojure.string :as string]
   [cheshire.core :as json]
   [clojure.java.shell :as sh]))

(defn i3-msg [& args]
  (apply sh/sh "i3-msg" args))

(defn workspaces []
  (-> (i3-msg "-t" "get_workspaces")
      :out
      (json/parse-string true)))

(defn workspace-name
  "Returns a simple workspace name for the focused workspace."
  []
  (->> (workspaces)
       (filter :focused)
       first
       :name
       (#(string/split % #":"))
       second
       (string/trim)))

(defn workspace-number
  []
  (->> (workspaces)
       (filter :focused)
       first
       :name
       (#(string/split % #":"))
       first))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; i3 Workspace Upsert
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn upsert [{:keys [name]}]
  (let [current-number (workspace-number)
        new-name       (str current-number ":" name)]
    (println "renaming workspace to" new-name)
    ;; (i3-msg "rename" "workspace" "to" name)
    ))
