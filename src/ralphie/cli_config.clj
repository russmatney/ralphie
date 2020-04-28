(ns ralphie.cli-config
  (:require
   [ralphie.dates :as dates]
   [ralphie.help :as help]
   [ralphie.screenshot :as screenshot]
   [ralphie.rofi :as rofi]
   [ralphie.term :as term]
   [ralphie.git :as git]
   [ralphie.install :as install]
   [ralphie.readme :as readme]
   [ralphie.workspace :as workspace]))

(def CONFIG
  {:commands [dates/command
              help/command
              screenshot/command
              rofi/command
              term/open
              git/clone
              install/command
              readme/build
              workspace/upsert]})
