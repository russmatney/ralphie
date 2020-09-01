(ns ralphie.emacs
  (:require
   [ralphie.workspace :as workspace]
   [clojure.java.shell :as sh]
   [ralphie.command :refer [defcom]]))

(defn open
  ([] (open (workspace/->current-workspace)))
  ([wsp]
   (let [;; these should be org/name and org/props
         name         (-> wsp :org/item :name)
         initial-file (-> wsp :org/item :props :initial-file)

         args ["emacsclient"
               "--no-wait"
               "--create-frame"
               "-F" (str "((name . \"" name "\"))")
               "--display=:0"
               "--eval"
               (str "(progn (russ/open-workspace \"" name "\") "
                    (if initial-file
                      (str "(find-file \"" initial-file "\")")
                      "")
                    ")")]]
     (apply sh/sh args))))

(comment
  (open (workspace/for-name "journal"))
  (open "yodo"))

(defn open-handler [_config parsed]
  (if-let [name (some-> parsed :arguments first)]
    (let [wsp (workspace/for-name name)
          wsp (or wsp (workspace/current-workspace))]
      (open wsp))
    (open)))

(defcom open-emacs
  {:name          "open-emacs"
   :one-line-desc "Opens emacs in the current workspace"
   :handler       open-handler})
