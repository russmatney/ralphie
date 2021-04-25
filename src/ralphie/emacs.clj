(ns ralphie.emacs
  (:require
   [ralphie.notify :refer [notify]]
   [ralphie.sh :as r.sh]
   [babashka.process :refer [$ check]]
   [ralph.defcom :refer [defcom]]
   [clojure.string :as string]
   [babashka.fs :as fs]
   ))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; emacs server/client fns
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

(def fallback-default-files
  ["readme.md"
   "readme.org"
   "deps.edn"
   "shadow-cljs.edn"
   "package.json"])

(defn determine-initial-file
  "Initial-file should be a file in the workspace repo's root.
  If it exists, it is returned.
  If it does not exist, sibling `fallback-default-files` are sought out.
  The first to exist is used.
  "
  [initial-file]
  (when initial-file
    (if (fs/exists? initial-file)
        initial-file
        (let [dir (fs/parent initial-file)
              lower-case-f->f (->> (fs/list-dir dir)
                                (map str)
                                (map (fn [f] [(string/lower-case f) f]))
                                (into {}))
              does-exist (->> fallback-default-files
                              (map #(str dir "/" %))
                              (filter lower-case-f->f)
                              first
                              lower-case-f->f)]
          does-exist))))

(comment
  (determine-initial-file "/home/russ/russmatney/ralphie/some.blah")
  (determine-initial-file "/home/russ/borkdude/babashka/readme.org")
  )

(defn open
  "Opens a new emacs client in the passed workspace.

  Uses the passed workspace data to direct emacs to the relevant initial file
  and named emacs workspace.
  "
  ([] (open nil))
  ([wsp]
   (let [wsp          (or wsp {})
         wsp-name
         (or (some wsp [:emacs.open/workspace :emacs/workspace-name
                        :workspace/title :org/name :clawe.defs/name])
             "ralphie-fallback")
         initial-file (some wsp [:emacs.open/file :emacs/open-file
                                 :workspace/initial-file :org.prop/initial-file])
         initial-file (determine-initial-file initial-file)
         elisp-hook   (:emacs.open/elisp-hook wsp)
         eval-str     (str
                        "(progn "
                        (when wsp-name
                          (str " (russ/open-workspace \"" wsp-name "\") "))
                        (when initial-file
                          (str " (find-file \"" initial-file "\") " " "))
                        (when elisp-hook elisp-hook)
                        " )")]

     (when-not (emacs-server-running?)
       (notify {:notify/subject          "Initializing Emacs Server, initializing."
                :notify/replaces-process "init-emacs-server"})
       (initialize-emacs-client)
       (notify {:notify/subject          "Started Emacs Server"
                :notify/replaces-process "init-emacs-server"}))

     (-> ($ emacsclient --no-wait --create-frame
            -F ~(str "((name . \"" wsp-name "\"))")
            --display=:0
            --eval ~eval-str)
         check))))

(comment
  (open {:emacs.open/workspace "ralphie"
         :emacs.open/file      "/home/russ/russmatney/ralphie/readme.org"}))

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
