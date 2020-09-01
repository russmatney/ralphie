(ns ralphie.term
  (:require
   [ralphie.tmux :as tmux]
   [ralphie.workspace :as workspace]
   [ralphie.command :refer [defcom]]
   ))

(defn open-term
  ([] (open-term (workspace/->current-workspace)))
  ([wsp]
   (let [wsp  (if (string? wsp) (workspace/for-name wsp)
                  wsp)
         opts {:name (-> wsp :org/item :name)}
         opts (if-let [dir (-> wsp :org/item :props :directory)]
                (assoc opts :directory dir)
                opts)]
     (println opts)
     (tmux/open-session opts))))

(comment
  (println "hi")
  (open-term "notes")
  (open-term "ralphie")
  (open-term "gamedev")
  )

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
