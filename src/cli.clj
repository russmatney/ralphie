(ns cli
  (:require
   [clojure.tools.cli :refer [parse-opts]]))

(defn screenshot []
  (println "take screenshot!"))

(defn print-help []
  (println "print help"))

(def config
  {:app      {:command     "-"
              :description "Cli-bindings"}
   :commands [{:command       "--help"
               :short         "-h"
               :one-line-desc "Prints help"
               :description   ["Takes a screenshot"]
               :runs          print-help}
              {:command       "--screenshot"
               :short         "-s"
               :one-line-desc "Take Screenshot"
               :description   ["Takes a screenshot"]
               :runs          screenshot}]})

(def opts ;; note, these are really commands, not opts....
  (into []
        (map
          (fn [{:keys [command short one-line-desc runs description] :as cmd}]
            [short command one-line-desc])
          (:commands config))))

(defn -main [& args]
  (let [parsed (parse-opts *command-line-args* opts)]

    (println (:summary parsed))))

(comment
  (def -res
    (-main '(screenshot)))

  (:summary -res)
  (-main '(-s -h))

  )
