#!/usr/bin/env -S bb --classpath src -m cli

(ns cli
  (:require
   [cli.dates :as dates]
   [cli.help :as help]
   [cli.screenshot :as screenshot]
   [cli.command :as command]
   [cli.rofi :as rofi]
   [clojure.test :as t :refer [is deftest]]
   [clojure.tools.cli :refer [parse-opts]]
   [clojure.set :as set]))

(def CONFIG
  {:commands [help/command
              screenshot/command
              rofi/command]})

(defn config->opts
  "Converts config into bb's parse-opt's expected form.
  see `clojure.tools.cli/parse-opts`"
  [config]
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

(deftest find-command-test
  (let [cmd (find-command CONFIG #{"help"})]
    (is (= "help" (:name cmd))))

  (is (= "screenshot" (:name (find-command CONFIG #{"screenshot"}))))
  (is (= nil (:name (find-command CONFIG #{"not-found"})))))

(defn call-command [config parsed]
  (if-let [command
           (find-command config
                         (set (:arguments parsed)))]
    (let [handler (:handler command)]
      (if handler
        (handler config parsed)
        (do
          (println "No handler for command" command)
          ((:handler help/command) config parsed))))
    (do
      (println "No command found for args" (:arguments parsed))
      ((:handler help/command) config parsed))))

(defn -main [& args]
  (let [config      CONFIG ;; preprocess config for all handlers first?
        ;; might be fun to build families of commands
        merged-args (apply (partial conj *command-line-args*) args)
        parsed      (parse-opts merged-args (config->opts config))
        debug       false]
    (println "\n\nNew Log running: " (dates/now))
    (when debug
      (prn "############")
      (println "*command-line-args*" *command-line-args*)
      (println "parsed input" (dissoc parsed :summary))
      (println "############\n"))
    (call-command config parsed)
    (when debug
      (println "\nSummary:\n" (:summary parsed)))))

(comment
  (def -res
    (-main "screenshot"))

  (:summary -res)
  )
