(ns ralphie.autojump
  (:require
   [clojure.string :as string]
   [ralphie.tmux :as tmux]
   [ralphie.config :as config]
   [ralphie.workspace :as workspace]
   [ralphie.rofi :as rofi]))

(def local-cache (str (config/project-dir) "/autojump.log"))

(defn autojump-handler
  "TODO: record and add history as suggestions"
  [_config _parsed]
  (let [xs    (map (fn [x] {:label x})
                   (string/split-lines (slurp local-cache)))
        input (rofi/rofi {:message   "Autojump input"
                          :on-select :label} xs)]
    (when-not (workspace/open? {:app "Alacritty"})
      (tmux/new-window))

    (when-not ((set (map :label xs)) input)
      (spit local-cache (str input "\n") :append true))

    (tmux/fire (str "j " input))))

(def autojump-cmd
  {:name          "autojump"
   :one-line-desc "Sends `j <userinput>` to the current workspace's tmux."
   :description   ["Uses j (autojump) to fuzzy-find a directory."
                   "Opens that directory in the workspace terminal."]
   :handler       autojump-handler})
