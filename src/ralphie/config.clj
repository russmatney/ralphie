(ns ralphie.config
  (:require [ralphie.sh :refer [expand]]))

;; TODO support configuration

(defn home-dir [] (expand "~"))

(defn project-dir [] (expand "~/russmatney/ralphie"))

(defn github-username [] "russmatney")

;; TODO handle multiple monitors
(defn monitor [] "eDP-1")
;; (defn monitor [] "HDMI-0")

(defn org-dir [] (expand "~/todo"))
