(ns ralphie.cli-config
  (:require
   [ralphie.dates :as dates]
   [ralphie.help :as help]
   [ralphie.screenshot :as screenshot]
   [ralphie.rofi :as rofi]
   [ralphie.term :as term]
   [ralphie.tmux :as tmux]
   [ralphie.story :as story]
   [ralphie.find-deps :as find-deps]
   [ralphie.git :as git]
   [ralphie.doctor :as doctor]
   [ralphie.install :as install]
   [ralphie.update :as update]
   [ralphie.window :as window]
   [ralphie.autojump :as autojump]
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
              story/story-cmd
              git/clone-cmd
              doctor/checkup-cmd
              find-deps/find-deps-cmd
              install/command
              update/update-doom-cmd
              window/resize-window-cmd
              readme/build
              yodo/restart-cmd
              workspace/start-workspace-cmd
              workspace/rename-cmd
              workspace/scratchpad-push-cmd
              workspace/scratchpad-pop-cmd]})

(comment
  ((:handler term/open-term-cmd) nil nil))

