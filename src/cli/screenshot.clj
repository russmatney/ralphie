(ns cli.screenshot)

(defn command [args]
  (prn "Hello from screenshot command!" (when args args)))
