(ns ralphie.scratchpad
  (:require
   [ralphie.command :refer [defcom]]))

(defn toggle-scratchpad-handler
  ([] (toggle-scratchpad-handler nil nil))
  ([_config _parsed]
   ;; impl scratchpad logic
   ))

(defcom toggle-scratchpad-cmd
  {:name          "toggle-scratchpad"
   :one-line-desc "Toggles the passed scratchpad."
   :description   [""]
   :handler       toggle-scratchpad-handler})

(comment)
