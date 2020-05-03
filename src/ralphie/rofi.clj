(ns ralphie.rofi
  (:require
   [ralphie.sh :as sh]
   [clojure.string :as string]
   [ralphie.config :as config]
   [ralphie.command :as command]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; rofi-general
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn rofi
  "Expects `xs` to be a coll of maps with a `:label` key.
  `msg` is displayed to the user.
  Upon selection, if the user-input matches a label, that `x`
  is selected and retuned.
  If a no match is found, the user input is returned.
  If on-select is passed, it is called with the selected input.
  "
  ([opts] (rofi opts (:xs opts)))
  ([{:keys [msg message on-select]} xs]
   (let [labels (map :label xs)
         msg    (or msg message)

         res
         ;; TODO remove ralphie/sh dep
         (sh/sh "rofi" "-i" "-dmenu" "-mesg" msg "-sync" "-p" "*"
                :in (string/join "\n" labels))

         selected-label (:out res)]

     (when (seq selected-label)
       ;; TODO use index-by
       (let [selected-x (->> xs
                             (filter #(= selected-label (:label %)))
                             first)]
         (if selected-x
           (if-let [on-select (or (:on-select selected-x) on-select)]
             (on-select selected-x)
             selected-x)
           selected-label))))))


(comment
  (sh/sh
    "rofi" "-i" "-dmenu" "-mesg" "Select bookmark to open" "-sync" "-p" "*"
    :in "11  iiii\n22 IIIIII\n33 33333")

  (rofi
    {:msg
     "message"}
    [{:label "iii" :url "urlllll"}
     {:label "333" :url "something"}
     {:label "jkjkjkjkjkjkjkjkjkjkjkjkjkjkjk" :url "w/e"}
     {:label "xxxxxxxx" :url "--------------"}]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; cli/command
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn config->rofi-commands
  [config]
  (->> config
       :commands
       (filter (comp seq :name))
       (map #(assoc % :label (:name %)))))

(defn rofi-handler
  "Returns the selected xs if there is no handler."
  [config parsed]
  (->> config
       config->rofi-commands
       (rofi {:msg "All commands"})
       ((fn [cmd]
          (if cmd
            (command/call-handler cmd config parsed)
            (println "Selected:" cmd))))))

(def command
  {:name          "rofi"
   :one-line-desc "Select a command to run via rofi."
   :description   ["Open Rofi for each command."
                   "Fires the selected command."
                   "Expects rofi to exist on the path."]
   :handler       rofi-handler})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Suggestion helpers
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn zsh-history
  "Zsh history as rofi-xs.
  TODO cache/speed up/trim allowed entries
  "
  []
  (->> "/.zsh_history"
       (str (config/home-dir))
       slurp
       string/split-lines
       (map (fn [l] (some-> l (string/split #";" 2) second)))
       (remove nil?)
       (map (fn [l] {:label l :on-select :label}))))

(comment
  (zsh-history)
  (rofi {:msg "zsh history"} (zsh-history)))
