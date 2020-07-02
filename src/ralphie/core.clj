#!/bin/sh
#_(
   "cd" "/home/russ/russmatney/ralphie"
   "exec" "bb" "--classpath" "$(clojure -Spath)" "-m" "ralphie.core" "$@"
   )

;; TODO un-hardcode the dirname

(ns ralphie.core
  (:require
   [ralphie.emacs]
   [ralphie.autojump]
   [ralphie.dates :as dates]
   [ralphie.help]
   [ralphie.screenshot]
   [ralphie.rofi]
   [ralphie.term]
   [ralphie.tmux]
   [ralphie.story]
   [ralphie.find-deps]
   [ralphie.git]
   [ralphie.doctor]
   [ralphie.install]
   [ralphie.update]
   [ralphie.window]
   [ralphie.readme]
   [ralphie.yodo]
   [ralphie.workspace]
   [ralphie.cli :as cli]
   [clojure.string :as string]))

(defn debug-log [log]
  (when false
    (spit "/home/russ/russmatney/ralphie/log"
          (str log "\n")
          :append true)))

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
    (-main "rofi")
    )

  (:summary -res)

  (println *file*))

;; Local Variables:
;; mode: clojure
;; End:
