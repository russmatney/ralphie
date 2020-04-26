(ns cli
  (:require
   [cli.dates :as dates]
   [cli.help :as help]
   [cli.screenshot :as screenshot]
   [cli.command :as command]
   [clojure.test :as t :refer [is deftest]]
   [clojure.tools.cli :refer [parse-opts]]
   [clojure.set :as set]))

(def CONFIG
  {:commands [help/command screenshot/command]})

(defn config->opts
  "Converts config into bb's parse-opts expected form."
  [config]
  ;; note, these are really commands, not opts....
  (into []
        (map
          (fn [{:keys [short long one-line-desc]}]
            [short long one-line-desc])
          (:commands config))))

(defn ensure-set [s] (if set? s (set s)))

(defn matching-ks? [s1 s2]
  (set/subset? s2 s1))

(defn find-command [config search-keys]
  (->> config
       :commands
       (group-by command/->keys)
       (map (fn [[k v]] [k (first v)]))
       (into {})
       (filter (fn [[ks v]]
                 (when (matching-ks? ks search-keys) v)))
       first
       second))

(deftest find-test
  (let [cmd (find-command CONFIG #{"help"})]
    (is (= "help" (:name cmd)))))

(defn call-command [config parsed]
  (let [command (find-command config (set (:arguments parsed)))
        handler (:handler command)]
    (when-not handler
      (println "No :handler found for command:" command))
    (when handler
      (handler config parsed))))

(defn -main [& args]
  (let [config      CONFIG ;; preprocess config for all handlers first?
        ;; might be fun to build families of commands
        merged-args (apply (partial conj *command-line-args*) args)
        parsed      (parse-opts merged-args (config->opts config))
        debug       true]
    (println "\n\nNew Log running: " (dates/now))
    (when debug
      (prn "############")
      (println "*command-line-args*" *command-line-args*)
      (println "parsed input" (dissoc parsed :summary))
      (prn "############"))
    (call-command config parsed)
    (when debug
      (println "\nSummary:\n" (:summary parsed)))))

;; (-main "screenshot")
(-main "help")

(comment
  (def -res
    (-main '(screenshot)))

  (:summary -res)
  (-main '(-s -h))
  )
