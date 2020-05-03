(ns ralphie.yodo
  (:require
   [ralphie.rofi :as rofi]
   [ralphie.tmux :as tmux]))

(defn restart-handler [_config _parsed]
  (rofi/rofi {:msg "Restart what?"}
             [{:label     "Restart yodo server"
               ;; missed arity here does not get applied well
               :on-select #(do % (tmux/fire "sc --user restart yodo"))}
              {:label     "Restart yodo frontend"
               :on-select #(do % (tmux/fire "sc --user restart yodo-fe"))}]))

(comment
  (restart-handler nil nil))

(def restart-cmd
  {:name          "Restart Yodo"
   :one-line-desc "Restart the locally-running yodo servers."
   :description   []
   :handler       restart-handler})
