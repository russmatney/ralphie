;; (ns ralphie.fzf
;;   (:require
;;    [ralphie.command :refer [defcom]]
;;    [babashka.process :refer [$ process check]]
;;    [clojure.java.shell :as sh]))



;; (defn fzf-handler
;;   ([] (fzf-handler nil nil))
;;   ([_config _parsed]
;;    (->
;;      (process '[fzf]
;;               {:in  :inherit
;;                :out :inherit})
;;      check :out slurp)
;;    ))

;; (comment
;;   (->
;;     (process '[fzf]
;;              {:out :inherit})
;;     check :out slurp)
;;   (sh/sh "fzf" "hi\nthere\npeanut\nbear")
;;   (sh/sh "echo" "hi")
;;   )

;; (defcom fzf-cmd
;;   {:name          "fzf"
;;    :one-line-desc "An fzf via bb debugger"
;;    :handler       fzf-handler})

;; (require '[babashka.process :refer [$ process check]])

;; (->
;;   (process '[fzf]
;;            {:in  :inherit
;;             :out :inherit})
;;   check)
