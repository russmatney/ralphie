(ns ralphie.notify
  (:require [clojure.java.shell :as sh]))

(defn notify [{:keys [subject body]}]
  (sh/sh "notify-send" subject (str body)))

(comment
  (notify {:subject "subj" :body {:value "v" :label "laaaa"}})
  (notify {:subject "subj" :body "BODY"})
  (sh/sh "notify-send" "subj" "body"))
