(ns ralphie.term
  (:require
   [ralphie.tmux :as tmux]
   ;; [ralphie.workspace :as workspace]
   ))

(defn open-term-handler
  ([] (open-term-handler nil nil))
  ([_config _parsed]
   ;; when opened, move focus instead
   (tmux/new-window)
   ;; when opened the first time
   ;; (tmux/fire (str "j " (:name (workspace/current))))

   ))

(comment
  (open-term-handler))

(def open-term-cmd
  {:name          "open-term"
   :one-line-desc "Opens a terminal."
   :description   ["Hardcoded to alacritty and tmux."
                   "Opens tmux using the current i3 workspace name."]
   :handler       open-term-handler})
