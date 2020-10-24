(ns ralphie.cli
  (:require
   [ralphie.help :as help]
   [ralphie.doctor :as doctor]
   [ralphie.command :as command]
   [clojure.tools.cli :refer [parse-opts]]))

(defn config->opts
  "Converts config into bb's parse-opt's expected form.
  see `clojure.tools.cli/parse-opts`"
  [config]
  (into []
        (map
          (fn [{:keys [short long one-line-desc]}]
            [short long one-line-desc])
          (:commands config))))

(defn find-command [config arg]
  (->> config
       :commands
       (group-by :name)
       (map (fn [[k v]] [k (first v)]))
       (into {})
       (#(get % arg))))

(defn parse-command [config parsed]
  (let [args      (:arguments parsed)
        first-arg (first args)
        command   (find-command config first-arg)]
    {:command command
     :args    (assoc parsed :arguments (rest args))}))

(defn run [& passed-args]
  (let [config                 {:commands (command/commands)}
        parsed                 (parse-opts passed-args (config->opts config))
        {:keys [command args]} (parse-command config parsed)
        debug                  false]
    (when debug
      (doctor/checkup-handler config args))

    (when-not command
      (println "404! Command not found.")
      (help/help-handler config passed-args))

    (when command
      (command/call-handler command config args))))

(comment
  (->>
    (command/commands)
    (map :name))
  (def -res
    (run "help"))

  (:summary -res))
