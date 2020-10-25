(ns ralphie.install
  (:require
   [babashka.process :refer [$ check]]
   [ralphie.command :refer [defcom] :as command]
   [ralphie.notify :refer [notify]]
   [ralphie.config :as config]))

(defn symlink
  [source target]
  (-> ($ ln -s ~source ~target)
      check))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Install-dev
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn add-dev-bin-to-path []
  (let [executable (str (config/project-dir) "/src/ralphie/core.clj")]
    (symlink executable (str (config/home-dir) "/.local/bin/ralphie-dev"))))

(defn install-cmd [_config _parsed]
  (add-dev-bin-to-path))

(defcom command
  {:name          "install-dev"
   :one-line-desc "Installs ralphie-dev via symlink."
   :description
   ["Symlinks the project's src/ralphie.core.clj into ~/.local/bin/ralphie-dev"
    "This 'dev' command runs the latest version of the code everytime."
    "Useful for debugging."
    "Install and run the uberscript version for better performance."]
   :handler       install-cmd})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Build uberscript
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; bb -cp $(clojure -Spath) -m ralphie.core --uberscript ralphie-script.clj

(defn get-cp
  "Builds a classpath in a directory."
  [dir]
  (-> ^{:dir dir}
      ($ clojure -Spath)
      check :out slurp))

(defn build-uberscript []
  (notify "Re-Building Ralphie Uberscript")
  (let [cp (get-cp (config/project-dir))]
    (-> ^{:dir (config/project-dir)}
        ($ bb -cp ~cp -m ralphie.core --uberscript ralphie-script.clj)
        check)
    (notify "Ralphie Uberscript Rebuilt.")))

(comment
  (build-uberscript)
  (-> ^{:dir "/home/russ/dotfiles"} ($ ls) :out slurp))

(defn install-uberscript []
  (spit "/home/russ/.local/bin/ralphie"
        (str "#!/bin/sh
exec bb /home/russ/russmatney/ralphie/ralphie-script.clj $@"))
  ($ chmod +x ~(config/ralphie-bin-path))
  (notify "Re-created wrapper shell script"))

(defn build-uberscript-handler
  ([] (build-uberscript-handler nil nil))
  ([_config _parsed]
   (build-uberscript)
   (install-uberscript)))

(defcom build-uberscript-cmd
  {:name          "build-and-install-uberscript"
   :one-line-desc "Builds an uberscript for ralphie, and symlinks to `ralphie`"
   :description
   ["Builds an uberscript for ralphie, pre-resolving namespaces."
    "Gives a nice performance enhancement."
    "Acts as a 'release' of sorts."
    "Writes an executable to ~/.local/bin/ralphie"
    "Note that this script can fall behind the repo code, "
    "and may need to be updated regularly."]
   :handler       build-uberscript-handler})

(comment)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Micros
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; current micro creation process, to be automated

;; 1. add -main for desired command to ns (ex: ralphie.scratchpad)
;; (defn -main [& args]
;;   (toggle-scratchpad-handler nil {:arguments args}))
;; 2. build uberscript with this ns as the main
;; bb -cp $(clojure -Spath) -m ralphie.scratchpad --uberscript toggle-scratchpad.clj
;; 3. remove all comment forms
;; carve doesn't yet ignore comments - this increases yield from the next step
;; this should go away as a contribution to carve
;; 4. carve it up
;; clj -A:carve --opts '{:aggressive true :paths ["toggle-scratchpad.clj"]}'
;; 5. install it
;; create a wrapper sh on your path (see `install-uberscript` for example)
;; 6. profit

(defn install-toggle-scratchpad []
  (spit "/home/russ/.local/bin/toggle-scratchpad"
        (str "#!/bin/sh
exec bb /home/russ/russmatney/ralphie/toggle-scratchpad.clj $@"))
  ($ chmod +x "/home/russ/.local/bin/toggle-scratchpad")
  (notify "Re-created toggle-scratchpad wrapper script"))

(comment
  (install-toggle-scratchpad))

;; (defn -main [& _args]
;;   ((command/get-handler cmd) nil {:arguments *command-line-args*}))
;; (defn -main [& args]
;;   (toggle-scratchpad-handler nil {:arguments args}))

(defn carve [{:keys [dir paths]}]
  (let [opts {:report  {:format :text}
              :out-dir dir
              :paths   paths}]
    (->
      ^{:dir (config/project-dir)}
      ($ clj -A:carve --opts ~opts)
      check :out slurp
      ;; edn/read-string
      )))

(comment
  (carve
    {:dir   (str (config/project-dir) "/scratchpad")
     :paths [
             "src/ralphie/scratchpad.clj"
             "src/ralphie/command.clj"
             "src/ralphie/emacs.clj"
             "src/ralphie/workspace.clj"
             "src/ralphie/awesome.clj"
             "src/ralphie/notify.clj"
             "src/ralphie/config.clj"
             "src/ralphie/sh.clj"

             "src/ralphie/i3.clj"
             "src/ralphie/rofi.clj"

             ;; ugh, would rather exclude these
             ;; transitive deps, not technically required
             ]})

  ;; (defn -main [& args]
  ;;   (toggle-scratchpad-handler nil {:arguments args}))

  (println "hi")

  )

(defn create-minimal-uberscript []
  (notify "Creating minimal uberscript")
  (-> ^{:dir (config/project-dir)}
      ($ bb -cp ~(get-cp (config/project-dir))
         -m ralphie.core
         --uberscript some-uberscript.clj)
      check
      :out
      slurp)
  (notify "Created uberscript"))

(comment
  (create-minimal-uberscript))

(defn install-micro-handler
  ([] (install-micro-handler nil nil))
  ([config {:keys [arguments]}]
   (let [micro-name (first arguments)
         cmd        (command/find-command (:commands config) micro-name)]
     (if micro-name
       (do
         ;; get namespace of command
         ;; get requires for namespace
         ;; get misc required namespaces (core, cli, command: ralphie-core nsps)
         ;; use carve to write bare minimum to temp dir
         ;; create uberscript from that output
         ;; write to 'binary' based on included command name
         (prn "cmd")
         (prn cmd))
       (notify (str "No command found for micro name: " micro-name))))))

(comment
  (install-micro-handler
    {:commands (command/commands)}
    {:arguments ["toggle-scratchpad"]}))

(defcom install-micro-cmd
  {:name          "install-micro"
   :one-line-desc "Creates a slimmed down script based on a subset of commands."
   :description   ["Intended to create as small an uberscript as possible."
                   "Can be thought of as ejecting a command from the rest of ralphie,"
                   "but into a usable binary."
                   "Uses carve and static analysis to build a custom bundle of namespaces as an uberscript."
                   "Intended to support ui-scripts like move-focus and toggle-scratchpad."
                   "Needs to be fast."]
   :handler       install-micro-handler})

(comment)
