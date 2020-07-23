(ns ralphie.picom
  (:require
   [ralphie.command :refer [defcom]]
   [clojure.java.shell :as sh]))

(defcom inc-opacity-command
  {:name          "opacity-increase"
   :one-line-desc "Increases the opacity of the next app clicked"
   :handler
   (fn [_ _] (sh/sh "picom-trans" "-s" "+5"))})

(defcom dec-opacity-command
  {:name          "opacity-decrease"
   :one-line-desc "Decreases the opacity of the next app clicked"
   :handler
   (fn [_ _] (sh/sh "picom-trans" "-s" "-5"))})
