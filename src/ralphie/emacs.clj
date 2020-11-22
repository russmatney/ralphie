(ns ralphie.emacs
  (:require
   [ralphie.workspace :as workspace]
   [ralphie.notify :refer [notify]]
   [ralphie.sh :as r.sh]
   [babashka.process :refer [$ check]]
   [ralph.defcom :refer [defcom]]
   [clojure.string :as string]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn emacs-server-running? []
  (try
    (-> ($ emacsclient -a false -e 't')
        check :out slurp string/trim (= "t"))
    (catch Exception _e
      false)))

(defn initialize-emacs-client []
  (r.sh/zsh
    (str "emacsclient --alternate-editor='' --no-wait --create-frame"
         " -e '(delete-frame)'")))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Open emacs client for passed workspace
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn open
  "Opens a new emacs client in the passed workspace.

  Uses the passed workspace data to direct emacs to the relevant initial file
  and named emacs workspace.
  "
  ([] (open (workspace/current-workspace)))
  ([wsp]
   (let [wsp-name     (-> wsp :org/name)
         initial-file (-> wsp :org.prop/initial-file)]

     (when-not (emacs-server-running?)
       (notify "No emacs server running, initializing.")
       (initialize-emacs-client)
       (notify "Started emacs server"))

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

