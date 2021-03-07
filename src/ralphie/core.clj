(ns ralphie.core
  (:require
   [ralph.defcom :as defcom]

   [ralphie.awesome]
   [ralphie.browser]
   [ralphie.dates]
   [ralphie.doctor]
   [ralphie.dashboard]
   [ralphie.emacs]
   [ralphie.fzf]
   [ralphie.git]
   [ralphie.help]
   [ralphie.install]
   [ralphie.item]
   [ralphie.monitor]
   [ralphie.notify]
   [ralphie.picom]
   [ralphie.readme]
   [ralphie.rofi]
   [ralphie.screenshot]
   [ralphie.spotify]
   [ralphie.story]
   [ralphie.term]
   [ralphie.tmux]
   [ralphie.update]
   [ralphie.watch]
   [ralphie.window]
   [ralphie.yodo]
   [ralphie.zsh]))

(defn -main [& args]
  (when-let [debug false]
    (when debug
      (spit "/home/russ/russmatney/ralphie/log"
            (str args "\n")
            :append true)))
  (println "[RALPHIE] start" args)
  (let [start-time (System/currentTimeMillis)
        res        (apply defcom/run args)
        dt         (- (System/currentTimeMillis) start-time)]
    (println "[RALPHIE] complete" args "in" dt "ms")
    res))
