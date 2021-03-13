(ns ralphie.emacs
  (:require
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
  ;; TODO refactor to support {:emacs/open-file "", :emacs/open-hook ""}
  ;; or a similar api
  ([] (open {:emacs/workspace-name "ralphie-fallback"}))
  ([wsp]
   (let [wsp-name     (some wsp [:emacs/workspace-name :workspace/title :org/name :clawe.defs/name])
         initial-file (some wsp [:emacs/open-file :workspace/initial-file :org.prop/initial-file])
         eval-str     (str "(progn (russ/open-workspace \"" wsp-name "\") "
                           (when initial-file
                             (str "(find-file \"" initial-file "\")" " "))
                           ")")]

     (when-not (emacs-server-running?)
       (notify "No emacs server running, initializing.")
       (initialize-emacs-client)
       (notify "Started emacs server"))

     (notify "Attempting new emacs client" (str wsp-name " :: " initial-file))
     (-> ($ emacsclient --no-wait --create-frame
            -F ~(str "((name . \"" wsp-name "\"))")
            --display=:0
            --eval ~eval-str)
         check)
     (notify "Created new emacs client" wsp-name))))

(defn open-handler [_config parsed]
  (if-let [name (some-> parsed :arguments first)]
    (open {:emacs/workspace-name name})
    (open)))

(defcom open-emacs
  {:name          "open-emacs"
   :one-line-desc "Opens emacs in the current workspace"
   :handler       open-handler})


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; cli
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn emacs-cli [cmd]
  ;; (notify cmd)
  ;; (println cmd)
  (-> ^{:out :string}
      ($ emacsclient -a false -e ~cmd)
      check
      :out))

(comment
  (emacs-cli "(org-clock-menu)")
  (emacs-cli "(org-clock-last)")
  )

(defcom emacs-cli-cmd
  {:name          "emacs-cli"
   :one-line-desc "Passes the string to emacs."
   :handler
   (fn [_config parsed]
     (let [cmd (some-> parsed :arguments first)]
       (if cmd
         (emacs-cli cmd)
         (notify "Emacs cli called without a command"
                 "Expected an expression to pass to emacsclient via -e."))))})
