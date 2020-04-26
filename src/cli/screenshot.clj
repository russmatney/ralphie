(ns cli.screenshot)

(def command
  {:name          "screenshot"
   :short         "-s"
   :long          "--screenshot"
   :one-line-desc "Take Screenshot"
   :description   ["Takes a screenshot"]
   :handler
   (fn screenshot [_config _parsed]
     (prn "Hello from screenshot command!"))})
