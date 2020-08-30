(ns ralphie.yodo
  (:require
   [ralphie.command :refer [defcom]]
   [ralphie.rofi :as rofi]
   [ralphie.config :as config]
   [ralphie.tmux :as tmux]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; restart yodo
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

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
  {:name          "restart-yodo"
   :one-line-desc "Restart the locally-running yodo servers."
   :description   []
   :handler       restart-handler})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Start yodo widget
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; TODO this does not really work, b/c tmux/fire loads up the same tmux session.
;; Should move to a desktop entry so that the OS manages the process for us.
(defn start-widget
  [route]
  (tmux/fire (str "cd " (config/yodo-dir)
                  " && yarn run electron electron/main.js " route)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; start the yodo bar
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn start-bar-handler
  ([] (start-bar-handler nil nil))
  ([_config _parsed]
   (start-widget "bar")))

(defcom start-bar-cmd
  {:name          "start-bar"
   :one-line-desc "Create and place the Yodo Status bar."
   :handler       start-bar-handler})

(comment)
