(ns ralphie.config
  (:require [ralphie.sh :refer [expand]]))

;; TODO support some kind of configuration

(defn home-dir [] (expand "~"))

(defn project-dir [] (expand "~/russmatney/ralphie"))

(defn github-username [] "russmatney")

(defn monitor []
  (or (System/getenv "MONITOR")
      "HDMI-0"
      "eDP-1"))

(defn org-dir [] (expand "~/todo"))
(defn roam-dir [] (expand "~/Dropbox/roam"))
(defn blog-garden-dir []
  (expand "~/russmatney/blog-gatsby/content/posts/garden"))

(defn i3-dir [] (expand "~/.config/i3"))
