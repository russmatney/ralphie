(ns ralphie.screenshot)

(def command
  {:name          "screenshot"
   :one-line-desc "Take Screenshot"
   :description   ["Takes a screenshot."
                   "Not yet implemented."]
   :handler
   (fn screenshot [_config _parsed]
     (prn "Hello from screenshot command!"))})
