(ns ralphie.term
  (:require
   [ralphie.workspace :as workspace]
   [clojure.core.async :as async]
   [clojure.java.shell :as sh]))

(defn open-term
  ([] (open-term nil nil))
  ([_config _parsed]
   (let [{:keys [name]} (workspace/current)
         args
         ["tmux" "-c"
          (str "alacritty -e tmux -v new-session -A -s " name " & disown")]
         ]

     (async/thread
       (apply sh/sh args)))))

(def open
  {:name          "open-term"
   :one-line-desc "Opens a terminal."
   :description   ["Hardcoded to alacritty and tmux."
                   "Opens tmux using the current i3 workspace name."]
   :handler       open-term})
