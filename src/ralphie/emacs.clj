(ns ralphie.emacs
  (:require
   [ralphie.workspace :as workspace]
   [clojure.java.shell :as sh]))

(defn open [workspace-name]
  (let [args ["emacsclient"
              "--no-wait"
              "--create-frame"
              "--display=:0"
              "--eval"
              (str "(russ/open-workspace \"" workspace-name "\")")
              ]]
    (println args)

    (apply sh/sh args)))

(comment
  (open "cosmos"))

(defn open-handler [_config _parsed]
  (let [{:keys [name]} (workspace/current)]
    (open name)))

(def open-cmd
  {:name          "open-emacs"
   :one-line-desc "Opens emacs in the current workspace"
   :handler       open-handler})
