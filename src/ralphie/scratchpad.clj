(ns ralphie.scratchpad
  (:require
   [ralph.defcom :refer [defcom]]
   [ralphie.emacs :as emacs]
   [ralphie.workspace :as workspace]
   [ralphie.awesome :as awm]
   [ralphie.notify :refer [notify]]
   [babashka.process :refer [process check]]
   [clojure.string :as string]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Create client
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn create-client
  "Creates clients for a given workspace"
  [wsp]
  (let [wsp (cond
              (nil? wsp)    (workspace/current-workspace)
              (string? wsp) (workspace/for-name wsp)
              :else         wsp)
        first-client
        (or (:org.prop/first-client wsp)
            (cond
              (-> wsp :org.prop/exec)         "exec"
              ;; IDEA check file-type for special cases
              (-> wsp :org.prop/initial-file) "emacs"
              :else                           "none"))]

    (case first-client
      "emacs" (emacs/open wsp)
      "app"   (let [exec (-> wsp :org.prop/exec)]
                (notify "Starting new client" (-> wsp :org.prop/exec))
                (-> exec
                    (string/split #" ")
                    process
                    check)
                (notify "New client started" exec))
      "none"
      ;; NOTE maybe detect a readme in directories as well
      (notify "New workspace has no default client."
              "Try setting :initial-file or :exec"))))

(comment
  (create-client "journal")
  (create-client "org-crud")
  (create-client "yodo-app")
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
  ;; set all floating false
  ;; set this client ontop true
  ;; set this client floating true
  ;; focus this client

  (awm/awm-cli
    {:parse? false
     :pp?    false}
    (str
      ;; set all ontops false
      "for c in awful.client.iterate(function (c) return c.ontop end) do\n"
      "c.ontop = false; "
      "c.floating = false; "
      "end;"

      ;; set this client ontop true, and focus it
      "for c in awful.client.iterate(function (c) return c.window == "
      (-> client :window awm/->lua-arg)
      " end) do\n"
      "c.ontop = true; "
      "c.floating = true; "
      "_G.client.focus = c;"
      "end; ")))

(defn toggle-scratchpad
  ([] (toggle-scratchpad (workspace/current-workspace)))
  ([wsp]
   (let [wsp      (cond
                    (nil? wsp)    (workspace/current-workspace)
                    (string? wsp) (workspace/for-name wsp)
                    :else         wsp)
         wsp-name (-> wsp :org/name)
         tag      (-> wsp :awesome/tag)
         client   (some-> tag :clients first)]
     (cond
       ;; "found selected tag, client for:" wsp-name
       (and tag client (:selected tag))
       (if (:ontop client)
         ;; TODO also set client ontop false ?
         (awm/toggle-tag wsp-name)
         (ontop-and-focused client))

       ;; "found unselected tag, client for:" wsp-name
       (and tag client (not (:selected tag)))
       (do
         (awm/toggle-tag wsp-name)
         (ontop-and-focused client))

       ;; tag exists, no client
       (and tag (not client))
       (create-client wsp)

       ;; tag does not exist, presumably no client either
       (not tag)
       (do
         (awm/create-tag! wsp-name)
         (awm/toggle-tag wsp-name)
         (create-client wsp))))))

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
