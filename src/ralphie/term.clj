(ns ralphie.term
  (:require
   [ralphie.tmux :as tmux]
   [clojure.core.async :as async]))

(defn open-term
  ([] (open-term nil nil))
  ([_config _parsed]
   (async/thread (tmux/open))))

(def open
  {:name          "open-term"
   :one-line-desc "Opens a terminal."
   :description   ["Hardcoded to alacritty and tmux."
                   "Opens tmux using the current i3 workspace name."]
   :handler       open-term})
