(ns ralphie.rofi
  (:require
   [babashka.process :refer [$ process check]]
   [clojure.string :as string]
   [ralphie.config :as config]
   [ralphie.util :as util]
   [ralphie.zsh :as zsh]
   [ralph.defcom :as defcom :refer [defcom]]
   [ralphie.doctor :as doctor]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; rofi-general
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn escape-rofi-label [label]
  (string/escape label {\& "&amp;"}))

;; TODO Tests for this,especially that ensure the result is returned
(defn rofi
  "Expects `xs` to be a coll of maps with a `:label` key.
  `msg` is displayed to the user.

  Upon selection, if the user-input matches a label, that `x`
  is selected and retuned.

  If a no match is found, the user input is returned.
  If on-select is passed, it is called with the selected input.

  Supports :require-match? in `opts`.
  "
  ;; TODO move opts and xs over to :rofi/prefixed keys
  ([opts] (rofi opts (:xs opts)))
  ([{:keys [msg message on-select require-match?]} xs]
   (doctor/log "Rofi called with" (count xs) "xs.")
   (let [maps?  (-> xs first map?)
         labels (if maps? (->> xs
                               (map (some-fn :label :rofi/label))
                               (map escape-rofi-label)
                               ) xs)
         msg    (or msg message)

         selected-label
         ;; TODO nil-punny error handling here
         ;; (rather than throwing when nothing is selected)
         (some->
           ^{:in (string/join "\n" labels)}
           ($ rofi -i
              ~(if require-match? "-no-custom" "")
              -markup-rows
              -dmenu -mesg ~msg -sync -p *)
           check
           :out
           slurp
           string/trim)]
     (when (seq selected-label)
       ;; TODO use index-by, or just make a map
       (let [selected-x (if maps?
                          (->> xs
                               (filter #(= selected-label
                                           (escape-rofi-label
                                             ((some-fn :label :rofi/label) %))))
                               first)
                          selected-label)]
         (if selected-x
           (if-let [on-select (or ((some-fn :rofi/on-select :on-select)
                                   selected-x) on-select)]
             (on-select selected-x)
             selected-x)
           selected-label))))))


(comment
  (->
    ^{:in "11  iiii\n22 IIIIII\n33 33333"}
    ($ rofi -i -dmenu -mesg "Select bookmark to open" -sync -p *)
    check
    :out
    slurp)

  (rofi
    {:msg "message"}
    [{:label "iii" :url "urlllll"}
     {:label "333" :url "something"}
     {:label "jkjkjkjkjkjkjkjkjkjkjkjkjkjkjk" :url "w/e"}
     {:label "xxxxxxxx" :url "--------------"}])

  (rofi
    {:require-match? true
     :msg            "message"}
    [{:label "iii" :url "urlllll"}
     {:label "333" :url "something"}
     {:label "jkjkjkjkjkjkjkjkjkjkjkjkjkjkjk" :url "w/e"}
     {:label "xxxxxxxx" :url "--------------"}]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Suggestion helpers
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn zsh-history
  "Zsh history as rofi-xs.
  TODO cache/speed up/trim allowed entries
  "
  []
  (->> (zsh/history)
       (sort-by :timestamp >)
       (map (fn [{:keys [line]}]
              {:label line :on-select :label}))))

(comment
  (zsh-history)
  (rofi {:msg "zsh history"} (zsh-history)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; ralphie dev
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn rofi-dev-handler
  ([] (rofi-dev-handler nil nil))
  ([_config _parsed]
   (let [cp (util/get-cp (config/project-dir))]
     (->
       (process ["bb" "-cp" cp "-m" "ralphie.core" "rofi"]
                {:dir (config/project-dir)})
       check
       :out
       slurp))))

(comment
  (let [foo "bar"]
    (-> (process ["echo" foo])
        check
        :out
        slurp)))

(defcom rofi-dev-cmd
  {:name          "rofi-dev"
   :one-line-desc "Runs a dev version of rofi, using the local source code."
   :description   ["Runs rofi via ralphie's local code."
                   "Allows you to run code as you write it, without needing to
install or jump into a shell to test it."  ]
   :handler       rofi-dev-handler})

(comment)


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; cli/command
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn config->rofi-commands
  [config]
  (->> config
       :commands
       (filter (comp seq :name))
       (map #(assoc % :label
                    ;; TODO command icons
                    (str
                      "<span >" (:name %) " </span> "
                      "<span color='gray'>" (:one-line-desc %) "</span> "
                      "<span>"
                      (string/join " " (:description %))
                      "</span>;")))))

(defn rofi-handler
  "Returns the selected xs if there is no handler."
  [config parsed]
  (when-let [cmd (some->> config
                          config->rofi-commands
                          (rofi {:require-match? true
                                 :msg            "All commands"}))]
    (defcom/call-handler cmd config parsed)))

(comment
  (rofi-handler nil nil)
  (rofi-handler {:commands (defcom/commands)} nil)
  )

(defcom select-command-via-rofi
  {:name          "rofi"
   :one-line-desc "Select a command to run via rofi."
   :description   ["Open Rofi for each command."
                   "Fires the selected command."
                   "Expects rofi to exist on the path."]
   :handler       rofi-handler})
