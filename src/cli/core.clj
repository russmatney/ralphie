#!/usr/bin/env -S bb --classpath src -m cli.core

(ns cli.core
  (:require
   [cli.dates :as dates]
   [cli.cli :as cli]
   [clojure.string :as string]))


(defn -main [& args]
  (let [args (if (= args *command-line-args*)
               args
               (apply (partial conj *command-line-args*) args))
        args (if (and (some? args)
                      (string/ends-with? (first args) "core.clj"))
               (rest args)
               args)]
    (println "\n\nNew Log running: " (dates/now))

    (apply cli/run args)))

(comment
  (def -res
    (-main "screenshot"))

  (:summary -res)

  (println *file*)
  )
