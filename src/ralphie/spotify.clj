(ns ralphie.spotify
  (:require
   [defthing.defcom :refer [defcom] :as defcom]
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

(defn adjust-spotify-volume [arg]
  (let [volume-str    (cond
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
           :notify/body    (str "new volume: " (:volume spotify-input))
           :notify/id      "spotify-volume"})))))

(defcom spotify-volume
  (fn [_ & args]
    (let [arg (some-> args first)]
      (adjust-spotify-volume arg))))

(comment
  (notify/notify
    {:notify/subject "adjusting spotify-volume"
     :notify/body    "bod"
     :notify/id      "spotify-volume"})

  (defcom/exec spotify-volume "up")
  (adjust-spotify-volume "up"))
