(ns ralphie.update
  (:require
   [ralph.defcom :refer [defcom]]
   [ralphie.tmux :as tmux]))

(defn update-doom-handler [_config _parsed]
  (tmux/fire "doom sync && doom build"))

(defcom update-doom-cmd
  {:name          "update-doom"
   :one-line-desc "Fetch and rebuild Doom"
   :description   ["Updates doom emacs to the bleeding-edge latest."
                   "Good luck out there, sport!"]
   :handler       update-doom-handler})
