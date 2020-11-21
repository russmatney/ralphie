(ns ralphie.term
  (:require
   [ralphie.tmux :as tmux]
   [ralphie.workspace :as workspace]
   [ralph.defcom :refer [defcom]]))

(defn open-term
  ([] (open-term (workspace/current-workspace)))
  ([wsp]
   (let [wsp  (if (string? wsp) (workspace/for-name wsp) wsp)
         opts {:name (-> wsp :org/name)}
         opts (if-let [dir (-> wsp :org.prop/directory)]
                (assoc opts :directory dir)
                opts)]
     (tmux/open-session opts))))

(comment
  (open-term)
  (open-term "notes")
  (open-term "ralphie")
  (open-term "gamedev"))

(defn open-term-handler
  ([] (open-term-handler nil nil))
  ([_config parsed]
   (let [name (some-> parsed :arguments first)]
     (if name
       (open-term name)
       (open-term)))))

(comment
  (open-term-handler))

(defcom open-term-cmd
  {:name          "open-term"
   :one-line-desc "Opens a terminal."
   :description   ["Hardcoded to alacritty and tmux."
                   "Opens tmux using the current i3 workspace name."]
   :handler       open-term-handler})

(comment)
