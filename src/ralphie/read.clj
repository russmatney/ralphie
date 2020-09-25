(ns ralphie.read
  (:require
   [ralphie.rofi :as rofi]
   [ralphie.config :as config]
   [org-crud.core :as org-crud]
   [clojure.string :as string]
   [ralphie.command :refer [defcom]])
  (:import java.lang.ProcessBuilder))

(defn org-item->rofi-urls
  [item]
  (->> item :org/urls
       (map (fn [u]
              {:label
               (str (:org/name item)
                    (when-not (string/includes? (:org/name item) u)
                      (str " | " u)))
               :url u}))
       (into [])))

(defn url->open-in-ff [url]
  (-> (ProcessBuilder. ["firefox" url])
      (.inheritIO)
      (.start)))

(comment
  (url->open-in-ff "http://github.com"))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Reads handler
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn find-read-handler
  ([] (find-read-handler nil nil))
  ([_config _parsed]
   (let [reads (org-crud/path->flattened-items (config/reads-file))
         ct    (->> reads (mapcat :org/urls) count)]
     (some->> reads
              (filter (comp seq :org/urls))
              (mapcat org-item->rofi-urls)
              (rofi/rofi {:msg (str "Read Urls (" ct ")")})
              :url
              url->open-in-ff))))

(defcom find-read-cmd
  {:name          "find-read"
   :one-line-desc "Lists and opens a url from read.org"
   :description   ["Effectively a list of bookmarks."]
   :handler       find-read-handler})

(comment
  (println "hi"))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Watches handler
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn find-watch-handler
  ([] (find-watch-handler nil nil))
  ([_config _parsed]
   (let [watches (org-crud/path->flattened-items (config/watches-file))
         ct      (->> watches (mapcat :org/urls) count)]
     (some->> watches
              (filter (comp seq :org/urls))
              (mapcat org-item->rofi-urls)
              (rofi/rofi {:msg (str "Watch Urls (" ct ")")})
              :url
              url->open-in-ff))))

(defcom find-watch-cmd
  {:name          "find-watch"
   :one-line-desc "Lists and opens a url from watch.org"
   :description   ["Effectively a list of bookmarks."]
   :handler       find-watch-handler})

(comment
  (println "hi"))
