(ns ralphie.emacs
  (:require
   [ralphie.workspace :as workspace]
   [ralphie.notify :refer [notify]]
   [babashka.process :refer [$ check]]
   [ralph.defcom :refer [defcom]]))

(defn open
  ([] (open (workspace/current-workspace)))
  ([wsp]
   (let [wsp-name     (-> wsp :org/name)
         initial-file (-> wsp :org.prop/initial-file)]

     (notify "Attempting new emacs client" (:org/name wsp))
     (-> ($ emacsclient --no-wait --create-frame
            -F ~(str "((name . \"" wsp-name "\"))")
            --display=:0
            --eval
            ~(str "(progn (russ/open-workspace \"" wsp-name "\") "
                  (if initial-file
                    (str "(find-file \"" initial-file "\")") "") ")"))
         check)
     (notify "Created new emacs client" (:org/name wsp)))))

(comment
  (open (workspace/for-name "journal"))
  (open (workspace/for-name "ralphie")))

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

(comment)
