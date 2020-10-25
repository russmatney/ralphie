;; originally pulled from https://github.com/borkdude/babashka/blob/master/examples/outdated.clj
;; https://github.com/borkdude/babashka/blob/945de0c5be9640d2ca42039531b315b8773badc5/examples/outdated.clj#L1
;; 'Inspired by an idea from @seancorfield on Clojurians Slack'

(ns ralphie.outdated
  (:require
   [clojure.edn :as edn]
   [babashka.process :refer [$ check]]
   [ralphie.config :as config]
   [clojure.string :as str]))

(defn repo->deps [repo]
  (-> repo
      :name
      (#(str (config/home-dir) "/" % "/deps.edn"))
      slurp
      edn/read-string
      :deps))

(comment
  (repo->deps {:name "russmatney/yodo"}))

(defn with-release [deps]
  (zipmap (keys deps)
          (map #(assoc % :mvn/version "RELEASE")
               (vals deps))))

(defn deps->versions [deps]
  (let [tree      (-> ($ clojure -Sdeps ~(str {:deps deps}) -Stree)
                       check
                       :out
                       slurp)
        lines     (str/split tree #"\n")
        top-level (remove #(str/starts-with? % " ") lines)
        deps      (map #(str/split % #" ") top-level)]
    (reduce (fn [acc [dep version]]
              (assoc acc dep version))
            {}
            deps)))

(defn version-map [deps] (deps->versions deps))
(defn new-version-map [deps] (deps->versions (with-release deps)))

(defn check-deps-for-repo [repo]
  (let [deps (repo->deps repo)]
    (println "\nChecking deps for repo: " (:name repo))
    (doseq [[dep version] (version-map deps)
            :let          [new-version (get (new-version-map deps) dep)]
            :when         (not= version new-version)]
      (println dep "can be upgraded from" version "to" new-version))
    (println "\nFinished checking deps for repo: " (:name repo))))

(comment
  (check-deps-for-repo {:name "russmatney/yodo"}))
