(ns ralphie.scratchpad
  (:require
   [ralphie.command :refer [defcom]]
   [ralphie.emacs :as emacs]
   [ralphie.workspace :as workspace]
   [ralphie.awesome :as awm]
   [ralphie.sh :as r.sh]
   [clojure.string :as string]
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

      (-> wsp :org/item :props :desktop-entry)
      (let [entry (-> wsp :org/item :props :desktop-entry)]
        (r.sh/zsh "/usr/bin/gtk-launch" entry)
        ;; BOOM
        ;; (shutdown-agents) not sure how to kill just sh's hanging future
        )

      ;; NOTE does not release properly....
      ;; (-> wsp :org/item :props :exec)
      ;; (let [exec (-> wsp :org/item :props :exec)]
      ;;   (as-> exec s
      ;;     (string/split s #" ")
      ;;     (concat s ["--" "&" "disown"])
      ;;     (apply sh/sh s)))
      )))

(comment
  (create-client "org-crud")
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

(defn this-client-ontop-and-focused [client]
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
        wsp-name (-> wsp :org/item :name)
        tag      (-> wsp :awesome/tag)
        client   (some-> tag :clients first)]
    (cond
      (and tag client (:selected tag))
      (do
        (println "found selected tag, client for:" wsp-name)
        (if (:ontop client)
          ;; TODO also set client ontop false ?
          (toggle-tag wsp-name)
          (this-client-ontop-and-focused client)))

      (and tag client (not (:selected tag)))
      (do
        (println "found unselected tag, client for:" wsp-name)
        (toggle-tag wsp-name)
        (this-client-ontop-and-focused client))

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
