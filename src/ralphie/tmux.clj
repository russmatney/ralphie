(ns ralphie.tmux
  (:require
   [clojure.java.shell :as sh]
   [ralphie.rofi :as rofi]
   [ralphie.command :refer [defcom]]))

(defn ->fire-session-name [workspace-name]
  (str workspace-name "-fire"))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Tmux create background
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn has-session? [name]
  (->
    (sh/sh "tmux" "has-session" "-t" name)
    :exit
    (= 0)))

(comment
  (has-session? "new"))

(defn new-session [{:keys [name dir]}]
  (when name
    (->>
      ["tmux" "new-session"
       "-d" ;; do not attach
       "-c" (if dir dir "~") ;; set working dir
       "-s" name ;; session name
       "-n" name ;; window name
       ]
      (apply sh/sh))))

(defn create-background-session
  "Creates a tmux session in the background."
  [{:keys [name] :as opts}]
  (if (has-session? name)
    (println (str "Found session " name "."))
    (new-session opts)))

(comment
  (create-background-session {:name "mysess"}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; New window
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn open-session
  "Creates a session in a new alacritty window."
  ([] (open-session {:name "ralphie-fallback" :directory "~/."}))
  ([{:keys [name directory]}]
   (let [args ["tmux" "-c"
               (str "alacritty --title " name
                    " -e tmux new-session -A "
                    (when directory (str " -c " directory))
                    " -s "
                    name
                    " & disown")]]
     (println args)
     (apply sh/sh args))))

(comment
  (open-session {:name "yodo"}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Fire command
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn fire
  "Aka send-keys.
  Defaults to firing into a 'tmux' workspace, which results in the
  'tmux-fire' session.

  Useful in that the output is accessible in a tmux session, somewhere.

  I'm tempted to mark it DEPRECATED in favor of using bb/process.
  "
  ([cmd-str]
   (fire cmd-str {:session "ralphie-fallback"}))
  ([cmd-str opts]
   (let [name (:session opts)]
     (create-background-session {:name name})
     (println "sessions created?")
     (sh/sh "tmux" "send-keys"
            "-t"  (str name ".0")
            ;; "-t"  name
            cmd-str
            "C-m"))))

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
