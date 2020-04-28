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
  (println "cosmos/sh. Err: " msg) ; todo colors
  ;; (notify/notify-send
  ;;   {:body    msg
  ;;    :subject "Cosmos `sh` Error"})
  )

(defn sh
  [& args]
  ;; (when args (println (string "cosmos/sh. Args: " args)))
  (let [result
        (try
          (apply clj-sh/sh args)
          (catch Exception e
            {:err (.getMessage e)}))
        out  (some-> result :out string/split-lines first-or-list)
        err  (some-> result :err string/split-lines first-or-list)
        exit (:exit result)]
    ;; (when out (println (string "cosmos/sh. Out: " out)))
    (when (not= "" err) (on-fail err))
    {:out out :err err :exit exit}))

(comment
  (sh "echo" "hi\nhi"))
