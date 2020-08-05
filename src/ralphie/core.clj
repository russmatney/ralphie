#!/bin/sh
#_(
   "cd" "/home/russ/russmatney/ralphie"
   "exec" "bb" "--classpath" "$(clojure -Spath)" "-m" "ralphie.core" "$@"
   )
;; TODO un-hardcode the dirname

(ns ralphie.core
  (:require
   [ralphie.autojump]
   [ralphie.awesome]
   [ralphie.cli :as cli]
   [ralphie.dates]
   [ralphie.doctor]
   [ralphie.emacs]
   [ralphie.deps]
   [ralphie.focus]
   [ralphie.git]
   [ralphie.help]
   [ralphie.install]
   [ralphie.item]
   [ralphie.items]
   [ralphie.monitor]
   [ralphie.notes]
   [ralphie.picom]
   [ralphie.pomodoro]
   [ralphie.prompts]
   [ralphie.readme]
   [ralphie.repos]
   [ralphie.rofi]
   [ralphie.screenshot]
   [ralphie.story]
   [ralphie.term]
   [ralphie.tmux]
   [ralphie.update]
   [ralphie.watch]
   [ralphie.window]
   [ralphie.workspace]
   [ralphie.yodo]
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
    ;; (println "\nNew Log running: " (dates/now))

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
