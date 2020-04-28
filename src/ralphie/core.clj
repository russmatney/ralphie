#!/bin/sh
"cd" "russmatney/ralphie"
"exec" "bb" "--classpath" "src" "-m" "ralphie.core" "$@"

(ns ralphie.core
  (:require
   [ralphie.dates :as dates]
   [ralphie.cli :as cli]
   [clojure.string :as string]))

(defn debug-log [log]
  (spit "/home/russ/russmatney/ralphie/log"
        (str log "\n")
        :append true))

(defn -main [& args]
  (let [args (if (= args *command-line-args*)
               args
               (apply (partial conj *command-line-args*) args))
        args (if (and (some? args)
                      (or
                        (string/ends-with? (first args) "core.clj")
                        (string/ends-with? (first args) "ralphie")))
               (rest args)
               args)]
    (debug-log args)
    (println "\nNew Log running: " (dates/now))

    (apply cli/run args)))

(comment
  (def -res
    (-main "screenshot"))

  (:summary -res)

  (println *file*)
  )
