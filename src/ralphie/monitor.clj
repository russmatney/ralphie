(ns ralphie.monitor
  (:require
   [ralphie.command :refer [defcom]]
   [ralphie.rofi :as rofi]
   [clojure.java.shell :as sh]))

;; TODO refactor rest of script into here, show dimensions in desc
(defn set-monitor-resolution-handler [_config {:keys [arguments]}]
  (let [label (some-> arguments first)
        label (or label (rofi/rofi {:msg       "Set monitor-resolution to:"
                                    :on-select :label}
                                   [{:label "laptop"}
                                    {:label "laptop-hr"}
                                    {:label "home"}
                                    {:label "home-monitor-only"}
                                    {:label "algo"}]))]
    (when label
      (sh/sh "set-monitor-config" label))))

(defcom set-monitor-resolution-cmd
  {:name          "set-monitor-resolution"
   :one-line-desc "set-monitor-resolution"
   :description   [""]
   :handler       set-monitor-resolution-handler})
