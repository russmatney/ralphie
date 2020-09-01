(ns ralphie.scratchpad
  (:require
   [ralphie.command :refer [defcom]]
   [ralphie.emacs :as emacs]
   [ralphie.workspace :as workspace]
   [clojure.java.shell :as sh]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Create client
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn create-client
  "Creates clients for a given workspace"
  [wsp]
  (let [wsp (cond
              (nil? wsp)    (workspace/current-workspace)
              (string? wsp) (workspace/for-name wsp)
              :else         wsp)]
    (cond
      (-> wsp :org/item :props :initial-file)
      (emacs/open wsp)

      (-> wsp :org/item :props :exec)
      (let [exec (-> wsp :org/item :props :exec)]
        (println "running workspace exec" exec)
        (sh/sh exec)))))

(defn create-client-handler
  ([] (create-client-handler nil nil))
  ([_config parsed]
   (if-let [arg (some-> parsed :arguments first)]
     (create-client arg)
     (create-client nil))))

(defcom create-client-cmd
  {:name          "create-client"
   :one-line-desc "Creates clients for the passed workspace name"
   :description   [""]
   :handler       create-client-handler})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; toggle scratchpad
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn toggle-scratchpad-handler
  ([] (toggle-scratchpad-handler nil nil))
  ([_config _parsed]
   ;; impl scratchpad logic
   ))

(defcom toggle-scratchpad-cmd
  {:name          "toggle-scratchpad"
   :one-line-desc "Toggles the passed scratchpad."
   :description   [""]
   :handler       toggle-scratchpad-handler})

(comment)
