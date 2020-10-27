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
  (let [handler (:handler opts)]
    `(let [ns#      ~(-> *ns* ns-name name)
           key#     ~(keyword (-> *ns* ns-name name) (name command-name))
           fn-name# ~(keyword (-> *ns* ns-name name) (name handler))
           opts#    (assoc ~opts
                           ::registry-key key#
                           :fn-name fn-name#
                           :ns ns#)]
       (swap! command-registry assoc key# opts#)
       nil)))

(defn commands [] (vals @command-registry))

(comment
  (println "hi"))

(defn find-command [commands command-name]
  (->> commands
       (group-by :name)
       (map (fn [[k v]] [k (first v)]))
       (into {})
       (#(get % command-name))))
