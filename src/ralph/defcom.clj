(ns ralph.defcom
  (:require
   [clojure.tools.cli :refer [parse-opts]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; registry
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defonce registry* (atom {}))

(defn clear-registry [] (reset! registry* {}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; defcom
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defmacro defcom
  "Registers a new command, with a name, documentation, dependencies, and configuration.

  The handler function is also set as a new function defined by the
  command-symbol, i.e.:

  ```
  (sut/defcom my-add {:name    \"my-add-com\"
                      :handler (fn [x] (+ x 1))})
  (my-add 3) ;; => 4
  ```

  Intended to create a data-driven command configuration for user actions."
  [command-symbol opts]
  (let [fn-symbol (symbol command-symbol)]
    `(let [handler# ~((some-fn :defcom/handler :handler) opts)]
       ;; set defcom name as name of handler
       ;; NOTE used by install-micro
       (def ~fn-symbol handler#)

       (let [ns#      ~(-> *ns* str)
             key#     ~(keyword (str *ns*) (name command-symbol))
             fn-name# ~(str *ns* "/" (name command-symbol))
             name#    ~(or (:name opts) (:defcom/name opts))
             opts#    (assoc ~opts
                             :defcom/name name#
                             ::registry-key key#
                             :defcom/handler-name fn-name#
                             :ns ns#)]

         (swap! registry* assoc key# opts#)

         ;; returns the created command map
         opts#))))

(comment
  (defcom example-cmd
    {:name    "example"
     :handler (fn [x] (+ x 2))})
  (example-cmd 2))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; call-handler
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn call-handler
  ;; TODO consider 'hotswap' - reevaluate defcom
  [cmd config parsed]
  ;; TODO perform pre-hooks, hydration, get-context, w/e
  (if-let [handler (and cmd ((some-fn :handler :defcom/handler) cmd))]
    (handler config parsed)
    ;; TODO hook into logger/notify/post-run-hook? throw?
    (println "No cmd or cmd without handler passed to call-handler" cmd)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; public helpers
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn list-commands
  "Lists all commands in the `registry*`"
  [] (vals @registry*))

(defn find-command
  "Returns the command with the matching `:defcom/name` (or deprecated `:name`)"
  [commands command-name]
  (let [by-name (->> commands
                     (group-by :defcom/name))]
    (doall
      (map (fn [[n grp]]
             (when (> 1 (count grp))
               (println (str "defcom/WARNING: non-unique :defcom/name detected: " n)
                        (map :ns grp)))
             ) by-name))
    (->> by-name
         (map (fn [[k v]] [k (first v)]))
         (into {})
         (#(% command-name)))))

(comment
  (find-command (list-commands) "example")
  )

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Cli run helper
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn parse-command [config parsed]
  (let [args      (:arguments parsed)
        first-arg (first args)
        command   (find-command (:commands config) first-arg)]
    {:command command
     :args    (assoc parsed :arguments (rest args))}))

(defn run [& passed-args]
  (let [config                 {:commands (list-commands)}
        parsed                 (parse-opts passed-args [])
        {:keys [command args]} (parse-command config parsed)]

    (if command
      (call-handler command config args)
      ;; TODO support registering a fallback command
      (println "404! Command not found."))))

(comment
  (->>
    (list-commands)
    (map :name))
  (def -res
    (run "help"))

  (:summary -res))
