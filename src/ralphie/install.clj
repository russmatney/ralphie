(ns ralphie.install
  (:require
   [babashka.process :refer [$ check]]
   [ralphie.command :refer [defcom] :as command]
   [ralphie.notify :refer [notify]]
   [ralphie.config :as config]
   [ralphie.rofi :as rofi]
   [ralphie.util :as util]))

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

(defn build-uberscript []
  (notify "Re-Building Ralphie Uberscript")
  (let [cp (util/get-cp (config/project-dir))]
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
;; Mini-uberscripts
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn temp-ns-path
  [_cmd] (str (config/src-dir) "/ralphie/temp.clj"))

(defn temp-uberscript-path
  [cmd] (str (config/project-dir) "/uberscripts/" (:name cmd) "_uberscript.clj"))

(defn command-bin-path [cmd]
  (str (config/local-bin-dir) "/" "ralphie-" (:name cmd)))

(defn write-temp-main-ns [cmd]
  (notify "Writing temp ns" (:name cmd))
  (spit (temp-ns-path cmd)
        (str "(ns ralphie.temp (:require ["
             (:ns cmd) "])) "
             "(defn -main [& args] ("
             (apply str (next (str (:fn-name cmd))))
             " nil {:arguments args}))"))
  (notify "Wrote temp ns" (:name cmd)))

(comment
  (apply str (next (str :some.name/space))))

(defn create-temp-uberscript [cmd]
  (notify "Creating temp uberscript" (:name cmd))
  (-> ^{:dir (config/project-dir)}
      ($ bb -cp ~(util/get-cp (config/project-dir))
         -m ralphie.temp
         --uberscript (temp-uberscript-path cmd))
      check
      :out
      slurp)
  (notify "Created temp uberscript" (:name cmd)))

(defn carve-temp-uberscript [cmd]
  (notify "Carving temp uberscript" (:name cmd))
  (let [opts {:paths            [(temp-uberscript-path cmd)]
              :aggressive       true
              :clj-kondo/config {:skip-comments true}}]
    (->
      ^{:dir (config/project-dir)}
      ($ clj -A:carve --opts ~opts)))
  (notify "Carved temp uberscript" (:name cmd)))

(defn install-temp-uberscript [cmd]
  (spit (command-bin-path cmd)
        (str "#!/bin/sh
exec bb " (temp-uberscript-path cmd) " $@"))
  ($ chmod +x (command-bin-path cmd))
  (notify "Created wrapper script" (:name cmd)))

(defn install-micro-handler
  ([] (install-micro-handler nil nil))
  ([config {:keys [arguments]}]
   (let [cmd (some-> arguments
                     first
                     (#(command/find-command (:commands config) %)))
         cmd (or cmd (rofi/rofi {:msg "Select command to install"}
                                (rofi/config->rofi-commands config)))]
     (notify (str "Installing micro handler for: " (:name cmd)) cmd)
     (if cmd
       (do
         ;; write dummy file with -main fn calling command's handler
         (write-temp-main-ns cmd)
         ;; create uberscript for new-file's namespace
         (create-temp-uberscript cmd)
         ;; carve file
         (carve-temp-uberscript cmd)
         ;; install bash wrapper to local/bin
         (install-temp-uberscript cmd)
         )
       (notify (str "No command selected for installation"))))))

(comment
  (install-micro-handler
    {:commands (command/commands)}
    {:arguments ["fire"]}))

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
