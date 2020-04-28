(ns ralphie.help)

(def command
  {:name          "help"
   :one-line-desc "Prints help"
   :description   ["Prints the known commands and the parsed input."]
   :handler
   (fn help [config parsed]
     (print "\nHelp Command Called\n")
     (print "\n\n#######\nParsed\n#######\n\n")
     (doall (map println parsed))
     (print "\n\n#######\nCommands\n#######\n\n")
     (doall (map (comp println
                       #(str "- " %)
                       :name) (:commands config))))})
