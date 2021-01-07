(ns ralph.cli
  (:require
   [ralphie.doctor :as doctor]
   [ralph.defcom :as defcom]
   [ralphie.fzf :as fzf]
   [clojure.tools.cli :refer [parse-opts]]))

(defn parse-command [config parsed]
  (let [args      (:arguments parsed)
        first-arg (first args)
        command   (defcom/find-command (:commands config) first-arg)]
    {:command command
     :args    (assoc parsed :arguments (rest args))}))

(defn run [& passed-args]
  (let [config                 {:commands (defcom/list-commands)}
        parsed                 (parse-opts passed-args [])
        {:keys [command args]} (parse-command config parsed)
        debug                  false]
    (when debug
      (doctor/checkup-handler config args))

    (if command
      (defcom/call-handler command config args)
      (do
        (println "404! Command not found. Falling back to fzf-select")
        ;; (help/help-handler config passed-args)
        (fzf/fzf-handler config passed-args)))))

(comment
  (->>
    (defcom/commands)
    (map :name))
  (def -res
    (run "help"))

  (:summary -res))
