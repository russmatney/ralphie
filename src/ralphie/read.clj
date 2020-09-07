(ns ralphie.read
  (:require
   [ralphie.rofi :as rofi]
   [ralphie.config :as config]
   [org-crud.core :as org-crud]
   [clojure.string :as string]
   [ralphie.command :refer [defcom]])
  (:import java.lang.ProcessBuilder))

(defn find-read-handler
  ([] (find-read-handler nil nil))
  ([_config _parsed]
   (let [reads (org-crud/path->flattened-items (config/reads-file))
         ct    (->> reads (mapcat :urls) count)]
     (some->> reads
              (mapcat
                (fn [r]
                  (->> r :urls
                       (map (fn [u]
                              {:label
                               (str (:name r)
                                    (when-not (string/includes? (:name r) u)
                                      (str " | " u)))
                               :url u}))
                       (into []))))
              (rofi/rofi {:msg (str "Read Urls (" ct ")")})
              :url
              ((fn [url]
                 (-> (ProcessBuilder. ["firefox" url])
                     (.inheritIO)
                     (.start))))))))


(defcom find-read-cmd
  {:name          "find-read"
   :one-line-desc "Lists and opens a url from read.org"
   :description   ["Effectively a list of bookmarks."]
   :handler       find-read-handler})

(comment
  (println "hi"))
