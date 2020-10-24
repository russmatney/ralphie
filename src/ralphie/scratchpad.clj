(ns ralphie.scratchpad
  (:require
   [ralphie.command :refer [defcom]]
   [ralphie.emacs :as emacs]
   [ralphie.workspace :as workspace]
   [ralphie.awesome :as awm]
   [ralphie.sh :as r.sh]
   [clojure.string :as string]
   [clojure.java.shell :as sh])
  (:import java.lang.ProcessBuilder))

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
      (-> wsp :org.prop/initial-file)
      (emacs/open wsp)

      (-> wsp :org.prop/exec)
      (let [exec (-> wsp :org.prop/exec
                     (string/split #" "))
            pb   (doto (ProcessBuilder. exec)
                   (.inheritIO))]
        (.start pb)))))

(comment
  (let [pb (ProcessBuilder. ["echo" "hi"])]
    (.start pb))

  (let [pb (doto (ProcessBuilder. ["gtk-launch" "yodo-electron.desktop"])
             (.inheritIO))]
    (.start pb))

  (sh/sh "exec" "sleep" "4")
  (r.sh/bash "nohup sleep 2")
  (create-client "journal")
  (create-client "org-crud")
  (create-client "yodo-app")
  ;; TODO web holds onto process - needs to be disowned somehow
  (workspace/for-name "web")
  (create-client "web"))

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

(defn ontop-and-focused [client]
  ;; set all ontops false
  ;; set this client ontop true
  ;; focus this client

  (awm/awm-cli
    {:parse? false
     :pp?    true}
    (str
      ;; set all ontops false
      "for c in awful.client.iterate(function (c) return c.ontop end) do\n"
      "c.ontop = false; "
      "end;"

      ;; set this client ontop true, and focus it
      "for c in awful.client.iterate(function (c) return c.window == "
      (-> client :window awm/->lua-arg)
      " end) do\n"
      "c.ontop = true; "
      "_G.client.focus = c;"
      "end; ")))

(defn toggle-tag [tag-name]
  ;; viewtoggle tag
  (awm/awm-cli
    (str "awful.tag.viewtoggle(awful.tag.find_by_name(s, \"" tag-name "\"));")))

(defn toggle-scratchpad [wsp]
  (let [wsp      (cond
                   (nil? wsp)    (workspace/current-workspace)
                   (string? wsp) (workspace/for-name wsp)
                   :else         wsp)
        wsp-name (-> wsp :org/name)
        tag      (-> wsp :awesome/tag)
        client   (some-> tag :clients first)]
    (cond
      (and tag client (:selected tag))
      (do
        (println "found selected tag, client for:" wsp-name)
        (if (:ontop client)
          ;; TODO also set client ontop false ?
          (toggle-tag wsp-name)
          (ontop-and-focused client)))

      (and tag client (not (:selected tag)))
      (do
        (println "found unselected tag, client for:" wsp-name)
        (toggle-tag wsp-name)
        (ontop-and-focused client))

      ;; tag exists, no client
      (and tag (not client))
      (do
        (println "tag, but no client:" wsp-name)
        (create-client wsp))

      ;; tag does not exist, presumably no client either
      (not tag)
      (do
        (awm/create-tag! wsp-name)
        (create-client wsp)))))

(comment
  (println "hi")
  (toggle-scratchpad "journal")

  (toggle-scratchpad "notes")
  (toggle-scratchpad "web")
  (workspace/for-name "web")
  )

(defn toggle-scratchpad-handler
  ([] (toggle-scratchpad-handler nil nil))
  ([_config parsed]
   (if-let [arg (some-> parsed :arguments first)]
     (toggle-scratchpad arg)
     (toggle-scratchpad nil))))

(defcom toggle-scratchpad-cmd
  {:name          "toggle-scratchpad"
   :one-line-desc "Toggles the passed scratchpad."
   :description   [""]
   :handler       toggle-scratchpad-handler})

(comment)