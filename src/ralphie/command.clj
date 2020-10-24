(ns ralphie.command)

(defn ->keys [{:keys [name short command aka]}]
  (set (filter seq (conj [] name short command aka))))

(defn ->rofi-x [cmd]
  {:label (:name cmd)
   :x     cmd})

(defn call-handler
  [cmd config parsed]
  (if (and cmd (:handler cmd))
    ((:handler cmd) config parsed)
    ;; TODO get logging lib
    (println "No cmd or cmd without handler passed to call-handler" cmd)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; defcom and command registry
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; TODO convert to multi-method
(defonce command-registry (atom {}))

(defmacro defcom
  [command-name opts]
  `(let [key#  ~(keyword (-> *ns* ns-name name) (name command-name))
         opts# (assoc ~opts ::registry-key key#)]
     (swap! command-registry assoc key# opts#)))

(defn commands [] (vals @command-registry))

(comment
  (println "hi"))
