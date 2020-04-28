(ns ralphie.install
  (:require [clojure.java.shell :as sh]))

(defn symlink
  [source target]
  (sh/sh "ln" "-s" source target))

(defn add-bin-to-path []
  ;; TODO un-hard-code, pull home/path dest from config?
  (let [executable "/home/russ/russmatney/ralphie/src/ralphie/core.clj"]
    (symlink executable "/home/russ/.local/bin/ralphie")))

(defn install-cmd [_config _parsed]
  (add-bin-to-path))

(def command
  {:name          "install"
   :short         "-i"
   :one-line-desc "Installs ralphie. Currently hard-coded :("
   :handler       install-cmd})
