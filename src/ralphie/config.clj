(ns ralphie.config
  (:require [ralphie.sh :refer [expand]]))

(defn home-dir [] (expand "~"))

(defn project-dir [] (expand "~/russmatney/ralphie"))
