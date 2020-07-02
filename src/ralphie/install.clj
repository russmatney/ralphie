(ns ralphie.install
  (:require
   [clojure.java.shell :as sh]
   [ralphie.command :refer [defcom]]
   [ralphie.config :as config]))

(defn symlink
  [source target]
  (sh/sh "ln" "-s" source target))

(defn add-bin-to-path []
  (let [executable (str (config/project-dir) "/src/ralphie/core.clj")]
    ;; TODO probably a better default bin dir
    (symlink executable (str (config/home-dir) "/.local/bin/ralphie"))))

(defn install-cmd [_config _parsed]
  (add-bin-to-path))

(defcom command
  {:name          "install"
   :one-line-desc "Installs ralphie via symlink."
   :description
   ["Symlinks the project's src/ralphie.core.clj into ~/.local/bin/ralphie"]
   :handler       install-cmd})
