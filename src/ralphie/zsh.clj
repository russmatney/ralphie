(ns ralphie.zsh
  (:require
   [ralphie.config :as config]
   [clojure.edn :as edn]
   [clojure.string :as string]))

;; TODO backup via merge and rewrite before clearing, in case you want full analytics
;; TODO remove history older than a day if exact copy of line
;; TODO remove history older than a week if same initial command and longer than 100 chars
;; TODO print analytics for removal as dry-run

(defn ->timestamp+duration [str]
  (let [[ts dur]
        (some-> str
                (string/replace ": " "")
                (string/split #":" 2))]
    (when (and ts dur)
      [(some-> ts edn/read-string)
       (some-> dur edn/read-string)])))

(comment
  (->timestamp+duration "")
  (->timestamp+duration ": 1576616542:0")
  (->timestamp+duration ": 1576616542:234"))

(defn history []
  (->> (str (config/home-dir) "/.zsh_history")
       slurp
       string/split-lines
       (map (fn [l]
              (let [entry (some-> l (string/split #";" 2))
                    line  (some-> entry second)
                    [timestamp duration]
                    (some-> entry first ->timestamp+duration)]
                (when (and line entry)
                  {:line      line
                   :duration  duration
                   :timestamp timestamp}))))
       (remove nil?)))

(comment
  (->>
    (history)
    (sort-by :timestamp >)))
