(ns ralphie.help
  (:require
   [ralph.defcom :refer [defcom]]))

(defn help-handler [config parsed]
  (print "\nHelp Command Called\n")
  (print "\n\n#######\nParsed\n#######\n\n")
  (doall (map println parsed))
  (print "\n\n#######\nCommands\n#######\n\n")
  (doall (map (comp println
                    #(str "- " %)
                    :name) (:commands config))))

(defcom command
  {:name          "help"
   :one-line-desc "Prints help"
   :description   ["Prints the known commands and the parsed input."]
   :handler       help-handler})
