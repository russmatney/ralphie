(ns cli.help)

(def command
  {:name          "help"
   :short         "-h"
   :long          "--help"
   :one-line-desc "Prints help"
   :description   ["Takes a screenshot"]
   :handler
   (fn help [config parsed]
     (print "\n\nHelp Command Called\n\n")
     (print "\n#######\nParsed\n#######\n\n")
     (doall (map println parsed))
     (print "\n#######\nCommands\n#######\n\n")
     (doall (map (comp println
                       #(str "- " %)
                       :name) (:commands config))))})
