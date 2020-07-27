(ns ralphie.term
  (:require
   [ralphie.tmux :as tmux]
   ;; [ralphie.workspace :as workspace]
   [ralphie.command :refer [defcom]]
   ))

(defn open-term-handler
  ([] (open-term-handler nil nil))
  ([_config _parsed]
   ;; TODO when opened, move focus via i3 instead?
   (tmux/open-session #_(workspace/->workspace))
   ))

(comment
  (open-term-handler))

(defcom open-term-cmd
  {:name          "open-term"
   :one-line-desc "Opens a terminal."
   :description   ["Hardcoded to alacritty and tmux."
                   "Opens tmux using the current i3 workspace name."]
   :handler       open-term-handler})
