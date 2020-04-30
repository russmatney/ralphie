(ns ralphie.cli-config
  (:require
   [ralphie.dates :as dates]
   [ralphie.help :as help]
   [ralphie.screenshot :as screenshot]
   [ralphie.rofi :as rofi]
   [ralphie.term :as term]
   [ralphie.emacs :as emacs]
   [ralphie.story :as story]
   [ralphie.git :as git]
   [ralphie.doctor :as doctor]
   [ralphie.install :as install]
   [ralphie.readme :as readme]
   [ralphie.workspace :as workspace]))

(def CONFIG
  {:commands [dates/command
              help/command
              screenshot/command
              rofi/command
              term/open
              emacs/open-cmd
              story/story-cmd
              git/clone-cmd
              doctor/checkup-cmd
              install/command
              readme/build
              workspace/upsert]})
