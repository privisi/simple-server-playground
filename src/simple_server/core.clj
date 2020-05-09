(ns simple-server.core
  (:require [clojure.pprint])
  (:require [ring.adapter.jetty :refer [run-jetty]]))


;;; URLs
;;;
;; In a shell, run this command:
;; curl -v  http://localhost:3001/some/long/path.html
;; We see that we now get this in our request map:
;;
;;  :uri "/some/long/path.html"
;;

;; So we can start thinking about how to organize our application
;; based on what urls are being asked for.
;;
;; Let's write a simple game, called "What number am I thinking?"
;; Computer will think of a number between 1 and 10, and we have to guess
;; what number it is.

(defn no-such-uri-response []
 {:status 404
  :headers {"Content-Type" "text/plain"}
  :body "Sorry, No such URI on this server!"})

(def game-in-progress (atom nil))

(defn new-game-response []
  ;; Make our new game:
  (reset! game-in-progress (+ 1 (rand-int 10)))
  {:status 200
   :headers {"Content-Type" "text/plain"}
   :body "OK- start guessing at /guess"})

(defn extract-guess [qs]
  (and qs
       (Long. (second (clojure.string/split qs #"=")))))

(defn guess-response [request]
  (if-let [guess (extract-guess (:query-string request))]
    (cond
      (= guess @game-in-progress)
      (and (reset! game-in-progress (+ 1 (rand-int 10)))
           {:status 200
            :headers {"Content-Type" "text/plain"}
            :body "Congratulations! You win!"})

      (< guess @game-in-progress)
      {:status 200
       :headers {"Content-Type" "text/plain"}
       :body "Too low."}

      (> guess @game-in-progress)
      {:status 200
       :headers {"Content-Type" "text/plain"}
       :body "Too high."})
    {:status 400                        ; Bad request
     :headers {"Content-Type" "text/plain"}
     :body "You need to supply a guess with /guess?guess=N"}))


(defn handler [request]
  (condp = (:uri request)
    "/new-game"  (new-game-response)
    "/guess"     (guess-response request)
    (no-such-uri-response)))


(defonce server
  (run-jetty #'handler {:port 3001 :join? false}))


:core
