(ns cli.command)

;; {:command       "--screenshot"
;;  :short         "-s"
;;  :one-line-desc "Take Screenshot"
;;  :description   ["Takes a screenshot"]
;;  :handler
;;  (fn screenshot [_config parsed]
;;    (prn "Hello from screenshot command!"
;;         (when parsed parsed)))}

(defn mk-command
  [{:keys [command short one-line-des description
           handler]
    :as   parsed}])


(defn ->keys [{:keys [name short command aka]}]
  (set (filter seq (conj [] name short command aka))))

(defn ->rofi-x [cmd]
  {:label (:name cmd)
   :x     cmd})

(defn call-handler [cmd config parsed]
  (let [handler (:handler cmd)]
    (if handler
      (handler config parsed)
      (println "No handler for command" cmd))))
