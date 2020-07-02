(ns ralphie.yodo
  (:require
   [ralphie.command :refer [defcom]]
   [ralphie.rofi :as rofi]
   [ralphie.tmux :as tmux]))

(defn restart-handler [_config _parsed]
  ;; TODO send to yodo workspace
  (rofi/rofi {:msg "Restart what?"}
             [{:label     "Restart yodo server"
               ;; missed arity here does not get applied well
               :on-select #(do % (tmux/fire "sc --user restart yodo"))}
              {:label     "Restart yodo frontend"
               :on-select #(do % (tmux/fire "sc --user restart yodo-fe"))}
              {:label     "Why not both?"
               :on-select #(do % (tmux/fire "sc --user restart yodo yodo-fe"))}]))

(comment
  (restart-handler nil nil))

(defcom restart-cmd
  {:name          "Restart Yodo"
   :one-line-desc "Restart the locally-running yodo servers."
   :description   []
   :handler       restart-handler})
