(ns ralphie.core
  (:require
   [ralph.defcom :as defcom]

   [ralphie.autojump]
   [ralphie.awesome]
   [ralphie.browser]
   [ralphie.dates]
   [ralphie.doctor]
   [ralphie.dashboard]
   [ralphie.emacs]
   [ralphie.deps]
   [ralphie.focus]
   [ralphie.fzf]
   [ralphie.git]
   [ralphie.help]
   [ralphie.install]
   [ralphie.item]
   [ralphie.items]
   [ralphie.monitor]
   [ralphie.notes]
   [ralphie.notify]
   [ralphie.picom]
   [ralphie.prompts]
   [ralphie.read]
   [ralphie.readme]
   [ralphie.repos]
   [ralphie.rofi]
   [ralphie.screenshot]
   [ralphie.scratchpad]
   [ralphie.spotify]
   [ralphie.story]
   [ralphie.term]
   [ralphie.tmux]
   [ralphie.update]
   [ralphie.watch]
   [ralphie.window]
   [ralphie.workspace]
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
