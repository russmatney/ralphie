(ns ralphie.tmux
  (:require
   [babashka.process :refer [$ check]]
   [ralphie.rofi :as rofi]
   [ralphie.notify :refer [notify]]
   [ralph.defcom :refer [defcom]]
   [ralphie.sh :as r.sh]
   [ralphie.config :as config]
   [clojure.string :as string]
   [ralphie.awesome :as awm]))

(defn list-windows
  "Parses `tmux list-windows -a` output into clojure maps.

  Fills:
  - :tmux.window/session-name
  - :tmux.window/index
  - :tmux.window/name
  "
  []
  (->
    ^{:out :string}
    ($ tmux list-windows -a)
    check :out string/split-lines
    (->>
      (map (fn [window-str]
             (-> window-str
                 (string/split #":" 3)
                 ((fn [[sesh-name window-idx rest]]
                    (let [name-str
                          (some-> rest
                                  string/trim
                                  (string/split #" " 2)
                                  first)
                          active?
                          (some->> name-str
                                   reverse
                                   (take 1)
                                   first
                                   #{\*}
                                   boolean)
                          window-name
                          (->> name-str
                               reverse
                               (drop 1)
                               reverse
                               (apply str))]
                      {:tmux.window/session-name sesh-name
                       :tmux.window/index        (read-string window-idx)
                       :tmux.window/name         window-name
                       :tmux.window/active?      active?})))))))))

(comment
  (list-windows))

(defn has-session? [name]
  (-> @($ tmux has-session -t ~name)
      :exit
      (= 0)))

(comment
  (has-session? "new")
  (has-session? "ralphie"))

(defn get-window
  [desc]
  (some->> (list-windows)
           (filter (fn
                     [{:tmux.window/keys [session-name name index]}]
                     (cond
                       (or
                         (and
                           (= (:tmux.window/session-name desc) session-name)
                           (= (:tmux.window/name desc) name))
                         (and
                           (= (:tmux.window/session-name desc) session-name)
                           (= (:tmux.window/index desc) index)))
                       true

                       :else false)))
           first))

(comment
  (get-window {:tmux.window/session-name "clawe"
               :tmux.window/name         "clawe"})
  (get-window {:tmux.window/session-name "clawe"
               :tmux.window/index        1})
  )

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; new session
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

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

(defn create-window
  "You should probably call `ensure-window`, which checks if the window
  already exists before creating."
  [{:keys [tmux.window/name
           tmux.window/session-name
           tmux.window/directory]}]

  (ensure-background-session {:name session-name :dir directory})
  (let [proc ($ tmux new-window
                -ad
                -c ~(if directory directory "~")
                -t ~session-name
                -n ~name)]
    (-> proc check :out slurp)))

(defn ensure-window [desc]
  (when-not (get-window desc)
    (create-window desc)))

(comment
  (get-window {:tmux.window/session-name "clawe"
               :tmux.window/name         "clawe"})
  (get-window {:tmux.window/session-name "clover"
               :tmux.window/index        1})
  (ensure-window {:tmux.window/session-name "clawe"
                  :tmux.window/name         "some-window"
                  :tmux.window/directory    "~/russmatney/clawe"}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Fire command
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn fire
  "Aka send-keys.
  Fires keys into the current workspace's 'tmux' session.

  Useful because:
  - the output is accessible in a tmux session, which might be desired
  - the workspace's session is usually in the expected directory already
  - commands can be fired from multiple places (emacs, a keybinding), and 'land'
  in the same place."
  ([cmd-str]
   (fire cmd-str nil))
  ([cmd-str opts]
   (let [opts         (or opts {})
         ;; should be a safe, default wm fallback
         current-tag  (awm/current-tag-name)
         session-name (or (some opts [:tmux/session-name]) current-tag)
         window-name  (or (some opts [:tmux/window-name]) current-tag)]
     (notify "ralphie/fire!" {:tmux/session-name session-name
                              :tmux/window-name  window-name
                              :tmux/cmd-str      cmd-str})

     ;; TODO not sure if this actually focuses the session before firing
     (ensure-window {:tmux.window/session-name session-name
                     :tmux.window/name         window-name})
     (->
       ;; could specify the directory here
       ($ tmux send-keys
          ;; .0 specifies the first window in the session
          -t ~(str session-name ":" window-name ".0")
          ~cmd-str C-m)
       check))))

(comment
  (fire "echo sup")
  (fire "echo sup"
        {:tmux/session-name "clawe"
         :tmux/window-name  "my-window"})
  )

(defn fire-handler [_config parsed]
  (let [cmd (or (->> parsed :arguments (string/join " "))
                (rofi/rofi {:msg "Command to fire"} (rofi/zsh-history)))]
    (fire cmd)))

(defcom fire-cmd
  {:name          "fire"
   :one-line-desc "Fires a command in the nearest tmux shell."
   :description   [""]
   :handler       fire-handler})

(comment
  (println "here")
  )

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; fire C-c
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn interrupt
  "Interrupts the command running in the first pane of the tmux session
  matching the current awesome tag name."
  []
  (let [name (awm/current-tag-name)]
    (notify "ralphie/interrupt!" {:name name})

    (-> ($ tmux send-keys "-t" ~(str name ".0") C-c)
        check)))

(defcom interrupt-cmd
  {:name    "interrupt"
   :handler (fn [_ _] (interrupt))})

(comment
  (interrupt))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; New window
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn open-session
  "Creates a session in a new alacritty window."
  ([] (open-session {:tmux/name "ralphie-fallback" :tmux/directory "~"}))
  ([{:tmux/keys [name directory] :as opts}]
   (let [directory (if directory
                     (r.sh/expand directory)
                     (config/home-dir))]
     ;; (notify "Creating new alacritty window with tmux session" opts)

     ;; NOTE `check`ing or derefing this won't release until
     ;; the alacritty window is closed. Not sure if there's a better
     ;; way to disown the process without skipping error handling
     (-> ($ alacritty --title ~name -e tmux "new-session" -A
            ~(when directory "-c") ~(when directory directory)
            -s ~name)))))

(comment
  (open-session {:tmux/name "name"})
  (open-session {:tmux/name "name" :tmux/directory "~/russmatney"}))
