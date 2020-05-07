(ns simple-server.core
  (:use ring.adapter.jetty))


(defn handler [request]
  (clojure.pprint/pprint request)
  {:status 200
   :headers {"Content-Type" "text/plain"}
   :body "Hello, class!"})


(def server
  (run-jetty #'handler {:port 3001 :join false}))
