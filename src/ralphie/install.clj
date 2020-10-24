(ns ralphie.install
  (:require
   [babashka.process :refer [$ check]]
   [ralphie.command :refer [defcom]]
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

(defn build-uberscript []
  (notify "Re-Building Ralphie Uberscript")
  (let [cp (-> ^{:dir (config/project-dir)}
               ($ clojure -Spath)
               check :out slurp)]
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
