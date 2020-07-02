(ns ralphie.screenshot
  (:require
   [ralphie.command :refer [defcom]]))

(defcom command
  {:name          "screenshot"
   :one-line-desc "Take Screenshot"
   :description   ["Takes a screenshot."
                   "Not yet implemented."]
   :handler
   (fn screenshot [_config _parsed]
     (prn "Hello from screenshot command!"))})
