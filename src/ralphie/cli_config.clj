(ns ralphie.cli-config
  (:require
   [ralphie.dates :as dates]
   [ralphie.help :as help]
   [ralphie.screenshot :as screenshot]
   [ralphie.rofi :as rofi]
   [ralphie.term :as term]
   [ralphie.emacs :as emacs]
   [ralphie.story :as story]
   [ralphie.find-deps :as find-deps]
   [ralphie.git :as git]
   [ralphie.doctor :as doctor]
   [ralphie.install :as install]
   [ralphie.update :as update]
   [ralphie.autojump :as autojump]
   [ralphie.readme :as readme]
   [ralphie.workspace :as workspace]))


(def CONFIG
  {:commands [autojump/autojump-cmd
              dates/command
              help/command
              screenshot/command
              rofi/command
              term/open-term-cmd
              emacs/open-cmd
              story/story-cmd
              git/clone-cmd
              doctor/checkup-cmd
              find-deps/find-deps-cmd
              install/command
              update/update-doom-cmd
              readme/build
              workspace/rename-cmd]})

(comment
  ((:handler term/open-term-cmd) nil nil))

