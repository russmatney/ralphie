(ns ralphie.spotify
  (:require
   [ralph.defcom :refer [defcom]]
   [ralphie.notify :as notify]
   [ralphie.pulseaudio :as pulseaudio]))


(defn spotify-sync-input
  "Sources and parses `pacmd list-sink-inputs`.
  Filters for the parsed running spotify input data.
  "
  []
  (some->>
    (pulseaudio/pacmd-sink-inputs)
    (filter (comp #{"RUNNING"} :state))
    (filter (comp #{"Spotify" "spotify"} :application.name :properties))
    first))

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
           (pulseaudio/pactl-set-sink-input-volume spotify-input volume-str)
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
