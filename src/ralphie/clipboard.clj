(ns ralphie.clipboard
  (:require
   [clojure.string :as string]
   [babashka.process :refer [$ check]]))

(defn get-clip [clipboard-name]
  (-> ($ xclip -o -selection clipboard-name)
      check
      :out
      slurp))

(defn get-all
  "Returns a list of things on available clipboards."
  []
  {:clipboard (get-clip "clipboard")
   :primary   (get-clip "primary")
   ;; :secondary (get-clip "secondary")
   ;; :buffer-cut (get-clip "buffer-cut")
   })

(defn values []
  (->> (get-all)
       vals
       (map string/trim)
       (remove empty?)))

(comment
  (values))
