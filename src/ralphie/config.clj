(ns ralphie.config
  (:require [ralphie.sh :refer [expand]]))

(defn home-dir [] (expand "~"))

;; TODO determine this at runtime
(defn project-dir [] (expand "~/russmatney/ralphie"))

;; TODO determine this at runtime
(defn github-username [] "russmatney")

;; TODO handle multiple monitors
;; (defn monitor [] "eDP-1")
(defn monitor [] "HDMI-0")
