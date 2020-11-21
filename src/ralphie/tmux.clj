(ns ralphie.tmux
  (:require
   [babashka.process :refer [$ check]]
   [ralphie.rofi :as rofi]
   [ralphie.notify :refer [notify]]
   [ralph.defcom :refer [defcom]]
   [ralphie.sh :as r.sh]
   [ralphie.config :as config]))

(defn ->fire-session-name [workspace-name]
  (str workspace-name "-fire"))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Tmux create background
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn has-session? [name]
  (-> @($ tmux has-session -t ~name)
      :exit
      (= 0)))

(comment
  (has-session? "new")
  (has-session? "ralphie"))

(defn new-session [{:keys [name dir] :as opts}]
  (notify "Attempt to create new tmux session" opts)
  (when name
    (-> ($ tmux new-session -d -c ~(if dir dir "~") -s ~name -n ~name)
        check
        :out
        slurp)
    (notify "Created new tmux session" opts)))

(comment
  (new-session {:name "new"})
  (has-session? {:name "ralphie"}))

(defn ensure-background-session
  "Creates a tmux session in the background."
  [{:keys [name] :as opts}]
  (if (has-session? name)
    (println (str "Found session " name "."))
    (new-session opts)))

(comment
  (ensure-background-session {:name "mysess"}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; New window
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn open-session
  "Creates a session in a new alacritty window."
  ([] (open-session {:name "ralphie-fallback" :directory "~"}))
  ([{:keys [name directory] :as opts}]
   (let [directory (if directory
                     (r.sh/expand directory)
                     (config/home-dir))]
     (notify "Creating new alacritty window with tmux session" opts)

     ;; NOTE `check`ing or derefing this won't release until
     ;; the alacritty window is closed. Not sure if there's a better
     ;; way to disown the process without skipping error handling
     (-> ($ alacritty --title ~name -e tmux "new-session" -A
            ~(when directory "-c") ~(when directory directory)
            -s ~name)))))

(comment
  (open-session {:name "name"})
  (open-session {:name "name" :directory "~/russmatney"}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Fire command
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn fire
  "Aka send-keys.
  Fires keys into a 'tmux' workspace, which defaults to the
  'ralphie-fallback' session.

  Useful in that the output is accessible in a tmux session, somewhere.

  Should probably be DEPRECATED in favor of using bb/process directly."
  ([cmd-str] (fire cmd-str {:session "ralphie-fallback"}))
  ([cmd-str opts]
   (let [session-name (:session opts)]
     (ensure-background-session {:name session-name})
     ($ tmux send-keys "-t"
        ;; .0 specifies the first window in the session
        ~(str session-name ".0")
        ~cmd-str C-m))))

(comment
  (fire "echo sup" {:session "ralphie"}))

(defn fire-handler [_config parsed]
  (let [cmd (or (some-> parsed :arguments first)
                (rofi/rofi {:msg "Command to fire"} (rofi/zsh-history)))]
    (fire cmd)))

(defcom fire-cmd
  {:name          "fire"
   :one-line-desc "Fires a command in the nearest tmux shell."
   :description   [""]
   :handler       fire-handler})

(comment)
