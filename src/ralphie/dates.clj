(ns ralphie.dates
  (:require
   [ralph.defcom :refer [defcom]]
   [ralph.defcom-2 :as d2]))

(defn now []
  (let [
        now         (java.time.ZonedDateTime/now)
        LA-timezone (java.time.ZoneId/of "America/Los_Angeles")
        LA-time     (.withZoneSameInstant now LA-timezone)
        pattern     (java.time.format.DateTimeFormatter/ofPattern "HH:mm")]
    (str "Time in LA:"
         (.format LA-time pattern))))

(defn date-cmd [_config _parsed]
  (now))

(defcom command
  {:name          "date"
   :one-line-desc "Prints the date"
   :handler       date-cmd})

(d2/defcom now-cmd
  (now))
(d2/exec now-cmd)
(d2/defcom date
  date-cmd)
(d2/exec date)
