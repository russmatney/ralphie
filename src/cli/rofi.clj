(ns cli.rofi
  (:require
   [cli.sh :as sh]
   [clojure.string :as string]
   [cli.command :as command]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; rofi-general
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn rofi
  [{:keys [xs msg on-select command]}]
  (let [labels (map :label xs)

        res
        (sh/sh "rofi" "-i" "-dmenu" "-mesg" msg "-sync" "-p" "*"
               :in (string/join "\n" labels))

        selected-label (:out res)]

    (if (empty? selected-label)
      "Nothing selected."
      (let [selected-x
            (->> xs
                 (filter #(= selected-label (:label %)))
                 first
                 )]
        (when command
          (command selected-x))
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

(defn rofi-cmd [config parsed]
  (let [commands     (:commands config)
        xs           (map command/->rofi-x commands)
        selected-cmd (:x (rofi {:xs  xs
                                :msg "All commands"}))]
    ;; TODO may want to dry or use command/ns for this
    ((:handler selected-cmd) config parsed)))

(def command
  {:name          "rofi"
   :short         "-f"
   :long          "--rofi"
   :one-line-desc "Open Rofi"
   :description   ["Open Rofi for each command"]
   :handler       rofi-cmd})
