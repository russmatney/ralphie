(ns ralphie.rofi
  (:require
   [ralphie.sh :as sh]
   [clojure.string :as string]
   [ralphie.command :as command]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; rofi-general
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn rofi
  ([opts] (rofi opts (:xs opts)))
  ([{:keys [msg message on-select]} xs]
   (let [labels (map :label xs)
         msg    (or msg message)

         res
         (sh/sh "rofi" "-i" "-dmenu" "-mesg" msg "-sync" "-p" "*"
                :in (string/join "\n" labels))

         selected-label (:out res)]

     (when (seq selected-label)
       (let [selected-x
             (->> xs
                  (filter #(= selected-label (:label %)))
                  first
                  )]
         (if on-select
           (on-select selected-x)
           selected-x))))))

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

(defn rofi-cmd
  "Returns the selected xs if there is no handler."
  [config parsed]
  (let [commands     (:commands config)
        ;; TODO get a warning/doctor check for when a :name is missing
        rofi-xs      (map #(assoc % :label (:name %))
                          (filter (comp seq :name) commands))
        selected-cmd (rofi {:xs  rofi-xs
                            :msg "All commands"})]
    (if selected-cmd
      (command/call-handler selected-cmd config parsed)
      (println "Selected:" selected-cmd))))


(def command
  {:name          "rofi"
   :one-line-desc "Select a command to run via rofi."
   :description   ["Open Rofi for each command."
                   "Fires the selected command."
                   "Expects rofi to exist on the path."]
   :handler       rofi-cmd})
