(ns ralphie.screenshot
  (:require
   [ralphie.command :refer [defcom]]
   [ralphie.rofi :as rofi]
   [clojure.java.shell :as sh]))

(defn select-region
  "Depends on ~/.local/bin/screenshot-region"
  []
  (sh/sh "screenshot-region"))

;; TODO optionally disable transparency before shot
(defn full-screen
  "Depends on ~/.local/bin/screenshot"
  []
  (sh/sh "screenshot"))

(defn take-screenshot [_config parsed]
  (let [arg (some-> parsed :arguments first)]
    (prn "Taking screenshot with arg" arg)
    (cond
      (= arg "full")
      (full-screen)

      (= arg "region")
      (select-region)

      :else
      (rofi/rofi {:msg "Full screen or select region?"}
                 [{:label     "full screen"
                   :on-select (fn [_arg] (full-screen))}
                  {:label     "select region"
                   :on-select (fn [_arg] (select-region))}]))))

(comment
  (take-screenshot nil nil))

(defcom take-screenshot-command
  {:name          "screenshot"
   :one-line-desc "Take Screenshot"
   :description   ["Takes a screenshot."
                   "Not yet implemented."]
   :handler       take-screenshot})

(comment
  (println "sup"))
