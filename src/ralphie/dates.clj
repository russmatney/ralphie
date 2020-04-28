(ns ralphie.dates)


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

(def command
  {:name          "date"
   :one-line-desc "Prints the date"
   :handler       date-cmd})
