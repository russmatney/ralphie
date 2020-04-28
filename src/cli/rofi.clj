(ns cli.rofi
  (:require
   [cli.sh :as sh]
   [clojure.string :as string]
   [cli.command :as command]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; rofi-general
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn rofi
  [{:keys [xs msg on-select]}]
  (let [labels (map :label xs)

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
          selected-x)))))

(comment
  (sh/sh
    "rofi" "-i" "-dmenu" "-mesg" "Select bookmark to open" "-sync" "-p" "*"
    :in "11  iiii\n22 IIIIII\n33 33333")

  (rofi
    {:xs [{:label "iii" :url "urlllll"}
          {:label "333" :url "something"}
          {:label "jkjkjkjkjkjkjkjkjkjkjkjkjkjkjk" :url "w/e"}
          {:label "xxxxxxxx" :url "--------------"}]}))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; cli/command
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn rofi-cmd
  "Returns the selected xs if there is no handler."
  [config parsed]
  (let [commands     (:commands config)
        selected-cmd (rofi {:xs  (map #(assoc % :label (:name %)) commands)
                            :msg "All commands"})]
    (if selected-cmd
      (command/call-handler selected-cmd config parsed)
      (println "Selected:" selected-cmd))))

(def command
  {:name          "rofi"
   :short         "-r"
   :long          "--rofi"
   :one-line-desc "Open Rofi"
   :description   ["Open Rofi for each command"]
   :handler       rofi-cmd})
