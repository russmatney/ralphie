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

  TODO support dependency validation, keybinding configuration, etc.

  Intended to create a data-driven command configuration for user actions."
  [command-symbol opts]
  (let [qualified-symbol (symbol (str *ns*) (name command-symbol))
        command-symbol   (with-meta (symbol (name command-symbol))
                           (meta command-symbol))
        ]
    `(let [handler# ~((some-fn :defcom/handler :handler) opts)]
       (def ~command-symbol handler#)

       (let [ns#   ~(-> *ns* str)
             key#  ~(keyword (-> *ns* str) (name command-symbol))
             name# ~(or (:name opts) (:defcom/name opts))
             opts# (assoc ~opts
                          :defcom/name name#
                          ::registry-key key#
                          :defcom/handler-name ~qualified-symbol
                          :fn-name ~qualified-symbol
                          :ns ns#)]

         (swap! registry* assoc key# opts#)

         ;; returns the created command map
         opts#))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; call-handler
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn call-handler
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
  (->> commands
       (group-by :defcom/name)
       (map (fn [[k v]] [k (first v)]))
       (into {})
       (#(get % command-name))))

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
