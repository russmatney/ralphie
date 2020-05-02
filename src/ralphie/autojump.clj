(ns ralphie.autojump
  (:require
   [ralphie.tmux :as tmux]
   [ralphie.workspace :as workspace]
   [ralphie.rofi :as rofi])
  )

(defn autojump-handler
  "TODO: record and add history as suggestions"
  [_config _parsed]
  (let [input (rofi/rofi {:message "Autojump input"})]
    (when-not
        (workspace/open? {:app :term})
      (tmux/open))
    (tmux/fire (str "j " input))))

(def autojump-cmd
  {:name          "autojump"
   :one-line-desc "Sends `j <userinput>` to the current workspace's tmux."
   :description   ["Uses j (autojump) to fuzzy-find a directory."
                   "Opens that directory in the workspace terminal."]
   :handler       autojump-handler})
