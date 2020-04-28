(ns ralphie.git
  (:require [clojure.java.shell :as sh]))

(def home-dir "/home/russ")

(defn clone-cmd [_config parsed]
  (let [repo (first (:arguments parsed))]
    (println repo)
    (sh/sh "hub" "clone" repo (str home-dir "/" repo))))

(def clone
  {:name          "clone"
   :short         "-"
   :one-line-desc "clone"
   :handler       clone-cmd})
