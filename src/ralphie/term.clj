(ns ralphie.term
  (:require
   [ralphie.tmux :as tmux]
   ;; [ralphie.workspace :as workspace]
   [ralphie.command :refer [defcom]]
   ))

(defn open-term-handler
  ([] (open-term-handler nil nil))
  ([_config parsed]
   (let [name (some-> parsed :arguments first)]
     (if name
       (tmux/open-session {:name name})
       (tmux/open-session)))))

(comment
  (open-term-handler))

(defcom open-term-cmd
  {:name          "open-term"
   :one-line-desc "Opens a terminal."
   :description   ["Hardcoded to alacritty and tmux."
                   "Opens tmux using the current i3 workspace name."]
   :handler       open-term-handler})
