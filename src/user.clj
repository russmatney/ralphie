(ns user
  (:require [cli.core :as cli]))

(defn go []
  (cli/-main "rofi"))
