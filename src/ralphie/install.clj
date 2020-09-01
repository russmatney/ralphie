(ns ralphie.install
  (:require
   [clojure.java.shell :as sh]
   [ralphie.command :refer [defcom]]
   [ralphie.config :as config]))

(defn symlink
  [source target]
  (sh/sh "ln" "-s" source target))

(defn add-dev-bin-to-path []
  (let [executable (str (config/project-dir) "/src/ralphie/core.clj")]
    ;; TODO probably a better default bin dir
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
  (let [cp (:out (sh/sh "clojure" "-Spath" :dir (config/project-dir)))]
    (sh/sh "bb"
           "-cp" cp
           "-m" "ralphie.core"
           "--uberscript" "ralphie-script.clj"
           :dir (config/project-dir))))

(defn install-uberscript []
  (spit "/home/russ/.local/bin/ralphie"
        (str "#!/bin/sh
cd /home/russ/russmatney/ralphie
exec bb ralphie-script.clj $@"))
  (sh/sh "chmod" "+x" "/home/russ/.local/bin/ralphie"))

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
