(ns ralphie.window
  (:require
   [ralphie.rofi :as rofi]
   [ralphie.i3 :as i3]
   ))

(def window-size-options
  [{:label  "small-centered"
    :i3-cmd "floating enable, resize set width 50 ppt height 50 ppt, move position center"}
   {:label  "large-centered"
    :i3-cmd "floating enable, resize set width 90 ppt height 90 ppt, move position center"}
   {:label  "tall-centered"
    :i3-cmd "floating enable, resize set width 40 ppt height 80 ppt, move position center"}
   ;; TODO handle position on multiple monitors
   ;; {:label  "right-side"
   ;;  :i3-cmd "floating enable, resize set width 45 ppt height 90 ppt, move position center"}
   ;; {:label  "left-side"
   ;;  :i3-cmd "floating enable, resize set width 40 ppt height 80 ppt, move position center"}
   ])

(defn resize-window-handler [_config _parsed]
  (->> window-size-options
       (rofi/rofi {:msg "Choose window layout type"})
       :i3-cmd
       (i3/i3-msg!)))

(def resize-window-cmd
  {:name          "resize-window"
   :one-line-desc "resize-window"
   :description
   ["Resizes the window according to a few presets." "Depends on i3." ]
   :handler       resize-window-handler})
