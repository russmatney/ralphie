(ns ralphie.spotify
  (:require
   [ralph.defcom :refer [defcom]]
   [ralphie.notify :as notify]
   [babashka.process :as process]
   [clojure.string :as string]
   [clojure.edn :as edn]))

;; https://askubuntu.com/questions/878853/change-spotify-volume-with-dbus
;; #!/bin/bash
;; spotify=$(pacmd list-sink-inputs | tr '\n' '\r' | perl -pe 's/ *index: ([0-9]+).+?application\.process\.binary = "([^\r]+)"\r.+?(?=index:|$)/\2 : \1\r/g' | tr '\r' '\n' | awk '/spotify/{print $3}')
;; step=2
;; pactl set-sink-input-volume $spotify +${step}%

(defn ->sink-input-props
  "Parses the properties: section of `pacmd list-sink-inputs`.
  Assumes it is passed a list of trimmed strs,
  with the properties: section as the tail."
  [strs]
  (->>
    strs
    (partition-by #(re-seq #"properties:" %))
    last
    (map (fn [s]
           (-> s
               (string/replace " =" "")
               (#(str ":" %)))))
    seq
    (string/join " ")
    (#(str "{" % "}"))
    edn/read-string))

(defn ->sink-data
  "Parses the non-properties section of `pacmd list-sink-inputs`.
  "
  [strs]
  (->>
    strs
    (partition-by #(re-seq #"properties:" %))
    first
    ;; here we drop lines without colons,
    ;; then parse into key/values using the first colon found
    (keep #(when (re-seq #":" %)
             (string/split % #": " 2)))
    (map (fn [[k v]]
           [(-> k
                (string/replace " " "-")
                keyword)
            v]))
    (into {})))

(defn ->sink-input [strs]
  (merge
    (->sink-data strs)
    {:properties (->sink-input-props strs)}))

(defn pacmd-sink-inputs []
  (->
    ^{:out :string}
    (process/$ pacmd list-sink-inputs)
    (process/check)
    :out
    (string/split #"\n")
    (->>
      (drop 1) ;; drop summary line
      (map string/trim)
      (partition-by #(re-seq #"^index:" %))
      (partition 2 2)
      (map (comp ->sink-input flatten)))))

(comment
  (pacmd-sink-inputs))

(defn spotify-sync-input
  "Sources and parses `pacmd list-sink-inputs`.
  Filters for the parsed running spotify input data.
  "
  []
  (some->>
    (pacmd-sink-inputs)
    (filter (comp #{"RUNNING"} :state))
    (filter (comp #{"Spotify" "spotify"} :application.name :properties))
    first))

;; pactl set-sink-input-volume $spotify +${step}%

(defn pactl-set-sink-input-volume
  "Updates a passed sinks volume with the indicated value.
  Expects to be handed a sink parsed by `pacmd-sink-inputs`.

  Volume-str should be something like +5, -10, where the int corresponds to a
  percentage increase or decrease."
  [{:keys [index]} volume-str]
  (->
    ^{:out :string}
    (process/$ pactl set-sink-input-volume ~index ~(str volume-str "%"))
    (process/check)
    :out))

(defcom spotify-volume
  {:defcom/name "spotify-volume"
   :defcom/handler
   (fn [_ parsed]
     (let [arg           (some-> parsed :arguments first)
           volume-str    (cond
                           (= "up" arg)   "+10"
                           (= "down" arg) "-10"
                           :else          nil)
           spotify-input (spotify-sync-input)]
       (if-not (and spotify-input volume-str)
         (do
           (println "spotify-volume missing params"
                    {:arg           arg :volume volume-str
                     :spotify-input spotify-input})
           (notify/notify "spotify-volume missing params"
                          {:arg           arg :volume volume-str
                           :spotify-input spotify-input}))
         (do
           (println "adjusting spotify-volume" {:arg arg :volume volume-str})
           (pactl-set-sink-input-volume spotify-input volume-str)
           (notify/notify
             {:notify/subject "adjusted spotify-volume"
              :notify/body (str "new volume: " (:volume spotify-input))
              :notify/id "spotify-volume"})))))})

(comment
  (notify/notify
    {:notify/subject "adjusting spotify-volume"
     :notify/body    "bod"
     :notify/id "spotify-volume"})

  (spotify-volume nil {:arguments ["up"]})
  (spotify-volume nil {:arguments ["down"]})
  (spotify-volume nil nil))
