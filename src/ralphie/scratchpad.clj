(ns ralphie.scratchpad
  (:require
   [ralphie.i3 :as i3]))

(defn scratchpad-show-handler
  [_config _parsed]
  (i3/i3-msg! "scratchpad show"))

(def scratchpad-show-cmd
  {:name          "scratchpad-show"
   :one-line-desc "Shows the next scratchpad."
   :description   ["Intended to get more convenient handling."
                   "Ex: per workspace, per app-type, all."]
   :handler       scratchpad-show-handler})
