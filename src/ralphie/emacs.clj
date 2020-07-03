(ns ralphie.emacs
  (:require
   [ralphie.workspace :as workspace]
   [clojure.java.shell :as sh]
   [ralphie.command :refer [defcom]]))

(defn open
  ([] (open nil))
  ([name]
   (let [name (or name (:name (workspace/->workspace)))
         args ["emacsclient"
               "--no-wait"
               "--create-frame"
               "--display=:0"
               "--eval"
               (str "(russ/open-workspace \"" name "\")")
               ]]
     (apply sh/sh args))))

(comment
  (open "yodo"))

(defn open-handler [_config _parsed] (open))

(defcom open-emacs
  {:name          "open-emacs"
   :one-line-desc "Opens emacs in the current workspace"
   :handler       open-handler})
