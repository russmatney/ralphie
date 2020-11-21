(ns ralphie.monitor
  (:require
   [ralph.defcom :refer [defcom]]
   [ralphie.rofi :as rofi]
   [babashka.process :refer [$ check]]))

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
      (-> ($ set-monitor-config ~label)
          check))))

(defcom set-monitor-resolution-cmd
  {:name          "set-monitor-resolution"
   :one-line-desc "set-monitor-resolution"
   :description   [""]
   :handler       set-monitor-resolution-handler})
