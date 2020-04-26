(ns user
  (:require [cli :as cli]))

(defn go []
  (cli/-main "rofi"))
