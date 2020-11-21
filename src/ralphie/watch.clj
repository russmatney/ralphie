(ns ralphie.watch
  (:require
   [ralph.defcom :refer [defcom]]
   ;; [babashka.pods :as pods]
   ))

;; (pods/load-pod "pod-babashka-filewatcher")
;; (require '[pod.babashka.filewatcher :as fw])

(defn watch-handler [_config parsed]
  (if-let [file (some->> parsed :arguments first)]
    (do
      (println "watching " file)
      ;; (fw/watch file (fn [event]
      ;;                  (println "do something")
      ;;                  (prn event))
      ;;           {:delay-ms 500})
      (println "should i hold onto fw?"))
    (println "No file passed.")))

(defcom watch-cmd
  {:name          "watch"
   :one-line-desc "watch"
   :description   ["Via Babashka file-watcher pod"]
   :handler       watch-handler})

(comment
  (fw/watch "/home/russ/russmatney/scratch/main.lua"
            (fn [event]
              (println "something")
              (prn event))
            {:delay-ms 50}))
