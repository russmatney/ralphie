(ns ralphie.picom
  (:require
   [ralphie.command :refer [defcom]]
   [ralphie.rofi :as rofi]
   [clojure.java.shell :as sh]))

(defcom inc-opacity-command
  {:name          "opacity-increase"
   :one-line-desc "Increases the opacity of the next app clicked"
   :handler
   (fn [_ _]
     (sh/sh "picom-trans" "-s" "+5"))})

(defcom dec-opacity-command
  {:name          "opacity-decrease"
   :one-line-desc "Decreases the opacity of the next app clicked"
   :handler
   (fn [_ _] (sh/sh "picom-trans" "-s" "-5"))})

(defcom picom
  {:name "picom"
   :handler
   (fn [_ _]
     (rofi/rofi
       {:msg "select"}
       [{:label     "Shout about it!"
         :on-select (fn [_]
                      (sh/sh "notify-send" "HOWDY WORLD!"))}
        {:label     "increase opacity"
         :on-select (fn [_] (sh/sh "picom-trans" "-s" "-5"))}
        {:label     "decrease opacity"
         :on-select (fn [_] (sh/sh "picom-trans" "-s" "+5"))}]))})
