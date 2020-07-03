(ns ralphie.sh
  (:require
   [clojure.java.shell :as clj-sh]
   [clojure.string :as string]))


(defn- first-or-list
  [x]
  (cond
    (string? x)     x
    (= 1 (count x)) (first x)
    (= [""] x)      nil
    :else           x))

(defn- on-fail [msg]
  (println "ralphie/sh. Err: " msg))

(defn sh
  [& args]
  (let [result
        (try
          (apply clj-sh/sh args)
          (catch Exception e
            {:err (.getMessage e)}))
        out  (some-> result :out string/split-lines first-or-list)
        err  (some-> result :err string/split-lines first-or-list)
        exit (:exit result)]
    (when (not= "" err) (on-fail err))
    {:out out :err err :exit exit}))

(comment
  (sh "echo" "hi\nhi"))

(defn bash [command]
  (clj-sh/sh "bash" "-c" command))

(defn zsh [& args]
  (apply clj-sh/sh "zsh" "-c" args))


(defn expand
  [path]
  (-> (str "echo -n " path)
      (bash)
      :out))
