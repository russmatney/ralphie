(ns ralphie.cli
  (:require
   [ralphie.help :as help]
   [ralphie.doctor :as doctor]
   [ralphie.command :as command]
   [ralphie.cli-config :refer [CONFIG]]
   [clojure.test :as t :refer [is deftest]]
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

(deftest find-command-test
  (let [cmd (find-command CONFIG "help")]
    (is (= "help" (:name cmd))))

  (is (= "screenshot" (:name (find-command CONFIG "screenshot"))))
  (is (= nil (:name (find-command CONFIG "not-found")))))

(defn parse-command [config parsed]
  (let [args      (:arguments parsed)
        first-arg (first args)
        command   (find-command config first-arg)]
    {:command command
     :args    (assoc parsed :arguments (rest args))}))

(defn run [& passed-args]
  (let [config                 (update CONFIG :commands
                                       concat (command/commands))
        parsed                 (parse-opts passed-args (config->opts config))
        {:keys [command args]} (parse-command config parsed)
        debug                  true
        ]
    (when debug
      (command/call-handler doctor/checkup-cmd config args))

    (when-not command
      (command/call-handler help/command config passed-args))

    (command/call-handler command config args)))

(comment
  (def -res
    (run "help"))

  (:summary -res))
