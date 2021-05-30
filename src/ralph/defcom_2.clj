(ns ralph.defcom-2
  (:require
   [defthing.core :as defthing]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; exec
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn exec
  "Executes a passed defcom, passing the command in as the first argument."
  [cmd & args]
  (apply (:defcom/fn cmd) cmd args))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; defcom-2
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defmacro defcom
  "Creates a command via defthing-like syntax.

  The last argument to a defcom is the command itself, and can be a named
  function, an anonymous function, or a clojure form that will be treated as the
  function body.

  The first arg passed is the built defcom (itself), and the rest are any
  command line args.

  Commands are registered for a few integrations by default,
  including ralph.rofi support.

  Every command gets a :name and :doc via defthing.
  "
  [command-name & xorfs]
  (let [f (last xorfs)
        command-fn
        ;; ensures the command is a function, and has the right arity
        (cond
          ;; named function, wrap if only [] arity
          (symbol? f)
          `(do
             (let [arglists# (:arglists (meta (var ~f)))]
               (cond
                 ;; named function with only zero arity
                 (and (-> arglists# count #{1}) (-> arglists# first count zero?))
                 ~(list 'fn '[& _rest] (list f))

                 ;; named function with only two arity
                 (and (-> arglists# count #{1}) (-> arglists# first count #{2}))
                 ~(list 'fn '[cmd & rst] (list f 'cmd 'rst))

                 :else
                 ~f)))

          ;; anonymous function, use it as-is
          (some-> f first (#{'fn 'fn*})) f

          ;; just a body, wrap as a variadic fn
          :else (list 'fn '[& _rest] f))
        xorfs (->> xorfs reverse (drop 1) reverse)]
    (apply defthing/defthing :defcom/command command-name
           (conj xorfs {:defcom/fn command-fn}))))

(comment
  ;; TODO convert to suite of tests
  (defcom example-cmd
    (fn [cmd & x]
      (println "adding 2 to passed x" x "in" (:name cmd))
      (+ (or (some-> x first) 1) 2)))
  (exec example-cmd 2)
  (exec example-cmd)

  (defcom cmd-no-args
    (fn [& _rest] (println "no args!")))
  (exec cmd-no-args)

  (defcom cmd-one-arg
    (fn [cmd] (println "one arg!" cmd)))
  (exec cmd-one-arg)

  (defcom cmd-no-args-2
    (println "no args!"))
  (exec cmd-no-args-2)

  (defn named-fn [] (println "named fn"))
  (defcom cmd-named-fn named-fn)
  (exec cmd-named-fn)
  (exec cmd-named-fn "blah")

  (defn named-fn-with-arg [arg1] (println "named fn args" arg1))

  (defcom cmd-named-one-arg named-fn-with-arg)
  (exec cmd-named-one-arg)

  (defcom cmd-anon-fn-shorthand-wrapper
    #(named-fn-with-arg (rest %&)))
  (exec cmd-anon-fn-shorthand-wrapper)
  (exec cmd-anon-fn-shorthand-wrapper "hiargs")

  (defn named-fn-with-two-args [arg1 arg2] (println "named fn two args" arg2 arg1))
  (defcom cmd-named-two-args named-fn-with-two-args)
  (exec cmd-named-two-args "hiargs")
  (exec cmd-named-two-args)
  )
