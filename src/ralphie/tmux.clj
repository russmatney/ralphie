(ns ralphie.tmux
  (:require
   [ralphie.workspace :as workspace]
   [clojure.java.shell :as sh]
   [ralphie.rofi :as rofi]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Fire command
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn fire
  "Aka send-keys
  TODO create the named workspace/session if it doesn't exist"
  ([cmd-str]
   (fire cmd-str {}))
  ([cmd-str opts]
   (let [wksp (or (:workspace opts) (:name (workspace/->workspace)))]
     (sh/sh "tmux" "send-keys" "-t"  wksp cmd-str "C-m"))))

(comment
  (fire "echo sup" {:workspace "dotfiles"}))

(defn fire-handler [_config parsed]
  (let [cmd (or (some-> parsed :arguments first)
                (rofi/rofi {:msg "Command to fire"} (rofi/zsh-history)))]
    (fire cmd)))


(def fire-cmd
  {:name          "fire"
   :one-line-desc "fire"
   :description   ["Fires a command in the nearest tmux shell."]
   :handler       fire-handler})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; New window
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn new-window []
  (let [{:keys [name directory]} (workspace/->workspace)
        args                     ["tmux" "-c"
                                  (str "alacritty -e tmux new-session -A "
                                       (when directory (str " -c " directory))
                                       " -s "
                                       name
                                       " & disown")]]
    (apply sh/sh args)))

(comment
  (new-window))
