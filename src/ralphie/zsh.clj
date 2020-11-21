(ns ralphie.zsh
  (:require
   [ralph.defcom :refer [defcom]]
   [ralphie.sh :as r.sh]
   [clojure.edn :as edn]
   [clojure.string :as string]
   [ralphie.fs :as fs]))

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

(defn history-file []
  (str (r.sh/expand "~") "/.zsh_history"))

(defn history
  "Parses zsh history into a map with `:line`, `:timestamp`, `:duration`*.

  Defaults to parsing ~/

  *NOTE :duration is likely not useful unless you've configured Zsh to record it.
  See: https://stackoverflow.com/questions/37961165/how-zsh-stores-history-history-file-format
  "
  ([] (history (history-file)))
  ([path]
   (if-not (fs/exists? path)
     (println (str "Warning: path does not exist " path ", no history found."))
     (->> path
          slurp
          string/split-lines
          (map (fn [l]
                 (let [entry (some-> l (string/split #";" 2))
                       line  (some-> entry second)
                       [timestamp duration]
                       (some-> entry first ->timestamp+duration)]
                   (when (and line (not (= "" line)))
                     {:line      line
                      :duration  duration
                      :timestamp timestamp}))))
          (remove nil?)))))

(comment
  (->>
    (history)
    (sort-by :duration >))
  (->>
    (history)
    (sort-by :timestamp >)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Write history
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn ->zsh-history-str [cmd]
  (when (:line cmd)
    (str ": " (:timestamp cmd) ":" (:duration cmd) ";" (:line cmd))))

(defn write-history [path cmds]
  (->> cmds
       (sort-by :timestamp <)
       (map ->zsh-history-str)
       (remove nil?)
       (string/join "\n")
       (spit path)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; create/update 'full' history
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn zsh-full-history-file []
  (str (r.sh/expand "~") "/.zsh_history.full"))

(defn by-timestamp [cmd]
  [(:timestamp cmd) cmd])

(defn update-full-zsh-history
  "Merges the current (minimal?) zsh history into the 'full' history.
  Intended to make later analysis possible, if you ever want it.
  "
  []
  (let [cmds     (history)
        full     (history (zsh-full-history-file))
        all-cmds (-> (merge
                       (->> cmds (map by-timestamp) (into {}))
                       (->> full (map by-timestamp) (into {})))
                     vals)]
    (write-history (zsh-full-history-file) all-cmds)))

(comment
  (update-full-zsh-history))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; clean up history
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn very-long-cmd? [cmd]
  (> (count (:line cmd)) 125))

;; (def example-count 10)

(defn print-dry-run-report [analysis]
  (doall
    (->> analysis
         :remove
         keys
         (map
           (fn [k]
             (when-let [found (-> analysis :remove (get k))]
               (println (str "Found " (-> found count) " " k " commands."))
               (->> found
                    ;; (take example-count)
                    (map
                      (fn [line]
                        (if (-> line count (>= 100))
                          (->> line (take 97)
                               (#(concat % "...")) (apply str))
                          line)))
                    (map println)
                    doall)
               ;; (println (str "along with "
               ;;               (- (-> found count) example-count)
               ;;               " others."))
               ))))))

(defn clean-up-zsh-history
  "Reads, analyzes, overwrites your ~/.zsh_history.
  Pass :dry-run to print a report."
  ([] (clean-up-zsh-history {:dry-run false}))
  ([{:keys [dry-run]}]
   (let [analysis
         (->> (history)
              (reduce
                (fn [agg next]
                  (cond
                    (very-long-cmd? next)
                    (update-in agg [:remove :very-long] conj (:line next))

                    (contains? (:lines agg) (:line next))
                    (update-in agg [:remove :duplicate] conj (:line next))

                    :else
                    (-> agg
                        (update :keep conj next)
                        (update :lines conj (:line next)))))
                {:keep   []
                 :lines  #{}
                 :remove {:very-long []
                          :duplicate #{}}}))]
     (if dry-run
       (print-dry-run-report analysis)
       (if (seq (:keep analysis))
         (write-history (history-file) (:keep analysis))
         (println "Warning: no commands found! Probably a problem..."))))))

(comment
  (clean-up-zsh-history))

(defn clean-up-zsh-handler
  ([] (clean-up-zsh-handler nil nil))
  ([_config _parsed]
   (update-full-zsh-history)
   (clean-up-zsh-history)
   nil))

(defcom clean-up-zsh-cmd
  {:name          "clean-up-zsh-history"
   :one-line-desc "Cleans up old, long zsh history commands"
   :description   ["Maintains a 'full' history, which may be used for later analysis."
                   "Removes long and duplicate commands."]
   :handler       clean-up-zsh-handler})

(defn clean-up-zsh-handler-dry-run
  ([] (clean-up-zsh-handler-dry-run nil nil))
  ([_config _parsed]
   (clean-up-zsh-history {:dry-run true})
   nil))

(defcom clean-up-zsh-dry-run-cmd
  {:name    "clean-up-zsh-history-dry-run"
   :handler clean-up-zsh-handler-dry-run})
