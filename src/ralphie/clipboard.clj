(ns ralphie.clipboard
  (:require
   [clojure.string :as string]
   [clojure.java.shell :as sh]))

(defn get-clip [clipboard-name]
  (-> (sh/sh "xclip" "-o" "-selection" clipboard-name) :out))

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
