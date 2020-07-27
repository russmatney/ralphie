(ns user
  (:require
   [babashka.pods :as pods]
   ;; [pod.babashka.filewatcher :as fw]
   [ralphie.core :as ralphie]))


(defn go []
  (ralphie/-main "rofi")
  )


(comment
  (println "hi")

  ;; (pods/load-pod "pod-babashka-filewatcher")

  ;; (fw/watch "/home/russ/russmatney/scratch/main.lua"
  ;;           (fn [event]
  ;;             (println "something")
  ;;             (prn event))
  ;;           {:delay-ms 50})
  )
