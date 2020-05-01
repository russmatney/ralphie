(ns ralphie.tmux
  (:require
   [ralphie.workspace :as workspace]
   [clojure.java.shell :as sh]))

(defn fire
  "Aka send-keys"
  ([cmd-str]
   (fire cmd-str {}))
  ([cmd-str opts]
   (let [wksp (or (:workspace opts) (:name (workspace/current)))]
     (sh/sh "tmux" "send-keys" "-t"  wksp cmd-str "C-m"))))

(comment
  (fire "echo sup" {:workspace "dotfiles"}))

(defn open []
  (let [{:keys [name]} (workspace/current)
        args
        ["tmux" "-c"
         (str "alacritty -e tmux -v new-session -A -s " name " & disown")]]

    (apply sh/sh args)))

(comment
  (open))
