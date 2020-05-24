(ns ralphie.tmux
  (:require
   [clojure.java.shell :as sh]
   [ralphie.rofi :as rofi]))

(defn ->fire-session-name [workspace-name]
  (str workspace-name "-fire"))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; New window
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn open-session [{:keys [name directory]}]
  (let [args ["tmux" "-c"
              (str "alacritty -e tmux new-session -A "
                   (when directory (str " -c " directory))
                   " -s "
                   name
                   " & disown")]]
    (apply sh/sh args)))

(comment
  (open-session {:name "yodo"}))

(defn ensure-sessions [{:keys [name]}]
  (when-not name
    (println "no workspace name provided" name))
  (when name
    (let [args ["tmux" "new-session" "-s" (->fire-session-name name)]]
      (apply sh/sh args))

    (let [args ["tmux" "new-session" "-s" name]]
      (apply sh/sh args))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Fire command
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn fire
  "Aka send-keys.
  Defaults to firing into a 'tmux' workspace, which results in the
  'tmux-fire' session."
  ([cmd-str]
   (fire cmd-str {:workspace {:name "tmux"}}))
  ([cmd-str opts]
   (let [name (:name (:workspace opts))]
     (ensure-sessions (:workspace opts))
     (println "sessions created?")
     (sh/sh "tmux" "send-keys"
            ;; "-t"  (->fire-session-name name)
            "-t"  name
            cmd-str
            "C-m"))))

(comment
  (fire "echo sup" {:workspace {:name "ralphie"}}))

(defn fire-handler [_config parsed]
  (let [cmd (or (some-> parsed :arguments first)
                (rofi/rofi {:msg "Command to fire"} (rofi/zsh-history)))]
    (fire cmd)))


(def fire-cmd
  {:name          "fire"
   :one-line-desc "Fires a command in the nearest tmux shell."
   :description   [""]
   :handler       fire-handler})
