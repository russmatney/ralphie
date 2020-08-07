(ns ralphie.config
  (:require [ralphie.sh :refer [expand]]))

;; TODO support some kind of configuration

(defn home-dir [] (expand "~"))

(defn project-dir [] (expand "~/russmatney/ralphie"))
(defn awesome-config-org-path [] (expand "~/russmatney/dotfiles/config.org"))

(defn github-username [] "russmatney")

(defn monitor []
  (or (System/getenv "MONITOR")
      "HDMI-0"
      "eDP-1"))

(defn org-dir [] (expand "~/todo"))
(defn repos-file [] (str (org-dir) "/repos.org"))
(defn projects-file [] (str (org-dir) "/projects.org"))
(defn goals-file [] (str (org-dir) "/goals.org"))
(defn today-file [] (str (org-dir) "/today.org"))
(defn journal-file [] (str (org-dir) "/journal.org"))
(defn prompts-file [] (str (org-dir) "/prompts.org"))
(defn pomodoros-file [] (str (org-dir) "/pomodoros.org"))
(defn screenshots-file [] (str (org-dir) "/screenshots.org"))

(defn item-paths []
  [(repos-file)
   (projects-file)
   (goals-file)
   (today-file)
   (journal-file)
   (prompts-file)
   (screenshots-file)])

(defn notes-dir [] (expand "~/Dropbox/notes"))
(defn blog-notes-dir []
  (expand "~/russmatney/blog-gatsby/content/posts/notes"))

(defn i3-dir [] (expand "~/.config/i3"))
