(ns ralphie.cli
  (:require
   [ralphie.dates :as dates]
   [ralphie.help :as help]
   [ralphie.screenshot :as screenshot]
   [ralphie.command :as command]
   [ralphie.rofi :as rofi]
   [ralphie.util :as util]
   [ralphie.term :as term]
   [ralphie.install :as install]
   [ralphie.workspace :as workspace]
   [clojure.test :as t :refer [is deftest]]
   [clojure.tools.cli :refer [parse-opts]]))

(def CONFIG
  {:commands [dates/command
              help/command
              screenshot/command
              rofi/command
              term/open
              install/command
              workspace/upsert]})


(defn config->opts
  "Converts config into bb's parse-opt's expected form.
  see `clojure.tools.cli/parse-opts`"
  [config]
  (into []
        (map
          (fn [{:keys [short long one-line-desc]}]
            [short long one-line-desc])
          (:commands config))))

(defn find-command [config search-keys]
  (->> config
       :commands
       (group-by command/->keys)
       (map (fn [[k v]] [k (first v)]))
       (into {})
       (filter (fn [[ks v]]
                 (when (util/matching-ks? ks search-keys) v)))
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
    (command/call-handler command config parsed)
    (do
      (println "No command found for args" (:arguments parsed))
      (command/call-handler help/command config parsed))))

(defn run [& args]
  (let [config CONFIG
        parsed (parse-opts args (config->opts config))]

    (call-command config parsed)))

(comment
  (def -res
    (run "help"))

  (:summary -res)
  (println *file*))
