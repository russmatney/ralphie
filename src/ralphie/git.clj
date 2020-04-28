(ns ralphie.git
  (:require [clojure.java.shell :as sh]
            [ralphie.config :refer [home-dir]]))

(defn clone [{:keys [repo-id]}]
  (sh/sh "hub" "clone" repo-id (str (home-dir) "/" repo-id)))

(defn clone-handler
  [_config parsed]
  (let [repo-id (first (:arguments parsed))]
    (clone repo-id)))

(def clone-cmd
  {:name          "clone"
   :one-line-desc "Git clone the passed repo into a sane location"
   :description
   ["When passed a repo-id, copies it into ~/repo-id."
    "Depends on `hub` on the command line."
    "Does not support private repos."]
   :handler       clone-handler})
