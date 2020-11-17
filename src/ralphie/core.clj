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
   [ralphie.story]
   [ralphie.term]
   [ralphie.tmux]
   [ralphie.update]
   [ralphie.watch]
   [ralphie.window]
   [ralphie.workspace]
   [ralphie.yodo]
   [ralphie.zsh]))


(defn debug-log [log]
  (when false
    (spit "/home/russ/russmatney/ralphie/log"
          (str log "\n")
          :append true)))

(defn -main [& args]
  (debug-log args)
  (apply cli/run args))
