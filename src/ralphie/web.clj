(ns ralphie.web
  (:require
   [ralph.defcom :refer [defcom]]
   [babashka.process :refer [$ check]]
   [ralphie.notify :as notify]))

(defn open-console-handler
  ([] (open-console-handler nil nil))
  ([_config _parsed]
   ;; TODO determine focused app
   ;; probably via awm-fnl/clawe
   ;; TODO dispatch to the client's open-console api
   ;; probably just a send-keys
   (notify/notify "open-console: Not Yet Impled")
   (-> ($ "echo" "open-console: Not Yet Impled")
       check
       :out
       slurp)))

(defcom open-console-cmd
  {:name          "open-console"
   :one-line-desc "Opens the dev console for the focused app."
   :description   ["Supports firefox, chrome, electron."]
   :handler       open-console-handler})
