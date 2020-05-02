(ns ralphie.command)

(defn ->keys [{:keys [name short command aka]}]
  (set (filter seq (conj [] name short command aka))))

(defn ->rofi-x [cmd]
  {:label (:name cmd)
   :x     cmd})

(defn call-handler
  [cmd config parsed]
  ((:handler cmd) config parsed))
