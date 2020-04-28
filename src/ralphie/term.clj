(ns ralphie.term
  (:require
   [ralphie.workspace :as workspace]
   [clojure.core.async :as async]
   [clojure.java.shell :as sh]))

(defn open-term
  ([] (open-term nil nil))
  ([_config _parsed]
   (let [{:keys [name]} (workspace/current)
         args
         ["tmux" "-c"
          (str "alacritty -e tmux -v new-session -A -s " name " & disown")]
         ]
     (println args)
     (async/thread
       (apply sh/sh args)))))

(comment
  (open-term)
  )

(def open
  {:name          "term"
   :short         "-t"
   :one-line-desc "term"
   :handler       open-term})

