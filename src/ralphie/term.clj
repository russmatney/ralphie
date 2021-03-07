(ns ralphie.term
  (:require
   [ralphie.tmux :as tmux]
   [ralph.defcom :refer [defcom]]))

;; TODO update to offering a more reasonable api (non-org based keys)
;; maybe {:term/name "" :term/open-in-directory "blah"}
;; support :term/open-hooks of some kind (e.g. print git status)
(defn open-term
  ([] (open-term {:term/name "ralphie-term" :term/directory "~"}))
  ([opts]
   (let [name ((some-fn [:org/name :term/name]) opts)
         dir ((some-fn [:org.prop/directory :term/directory]) opts)]
     ;; TODO refactor/consume/simplify the tmux api?
     (tmux/open-session {;; TODO namespace these keys
                         :tmux/name name
                         :tmux/directory dir}))))

(comment
  (open-term)
  (open-term "notes")
  (open-term "ralphie")
  (open-term "gamedev"))

(defn open-term-handler
  ([] (open-term-handler nil nil))
  ([_config parsed]
   (let [name (some-> parsed :arguments first)]
     (if name
       (open-term name)
       (open-term)))))

(comment
  (open-term-handler))

(defcom open-term-cmd
  {:name          "open-term"
   :one-line-desc "Opens a terminal."
   :description   ["Hardcoded to alacritty and tmux."
                   "Opens tmux using the current i3 workspace name."]
   :handler       open-term-handler})

(comment)
