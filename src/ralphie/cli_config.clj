(ns ralphie.cli-config
  (:require
   [ralphie.dates :as dates]
   [ralphie.help :as help]
   [ralphie.screenshot :as screenshot]
   [ralphie.rofi :as rofi]
   [ralphie.term :as term]
   [ralphie.tmux :as tmux]
   [ralphie.emacs :as emacs]
   [ralphie.story :as story]
   [ralphie.find-deps :as find-deps]
   [ralphie.git :as git]
   [ralphie.doctor :as doctor]
   [ralphie.install :as install]
   [ralphie.update :as update]
   [ralphie.autojump :as autojump]
   [ralphie.scratchpad :as scratchpad]
   [ralphie.readme :as readme]
   [ralphie.yodo :as yodo]
   [ralphie.workspace :as workspace]))

(def CONFIG
  {:commands [autojump/autojump-cmd
              dates/command
              help/command
              screenshot/command
              rofi/command
              term/open-term-cmd
              tmux/fire-cmd
              emacs/open-cmd
              story/story-cmd
              git/clone-cmd
              doctor/checkup-cmd
              scratchpad/scratchpad-show-cmd
              find-deps/find-deps-cmd
              install/command
              update/update-doom-cmd
              readme/build
              yodo/restart-cmd
              workspace/start-workspace-cmd
              workspace/rename-cmd]})

(comment
  ((:handler term/open-term-cmd) nil nil))

