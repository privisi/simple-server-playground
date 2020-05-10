(ns simple-server.core
  (:require [clojure.pprint]
            [ring.adapter.jetty :refer [run-jetty]]

            [ring.util.response :refer [response created redirect not-found status]]
            [compojure.core :refer [GET POST ANY defroutes]]
            [compojure.coercions :refer [as-int]]
            [ring.middleware.defaults :as middleware]

            [ring.mock.request :as mock]))


;;; Let's rewrite our game a bit more neatly
;;; with proper destructuring and middlewares.

(def game-in-progress (atom nil))

(defn new-game-handler []
  ;; Make our new game:
  (reset! game-in-progress (+ 1 (rand-int 10)))
  (response "OK- start guessing at /guess"))

(defn guess-handler [guess]
  (cond
    (nil? guess)
    ;; Notice, there is no helper for a 400 response, but we can
    ;; easily create one like this:
    (-> (response  "You need to supply a guess with /guess?guess=N")
        (status 400))

    (= guess @game-in-progress)
    (and (reset! game-in-progress (+ 1 (rand-int 10)))
         (response  "Congratulations! You win!"))

    (< guess @game-in-progress)
    (response "Too low.")

    (> guess @game-in-progress)
    (response  "Too high.")))

(defroutes game-routes
  (GET "/new-game" []                 (new-game-handler))
  (GET "/guess"    [guess :<< as-int] (guess-handler guess))
  (ANY "*"         []                 (not-found "Sorry, No such URI on this server!")))

(def handler
  (-> game-routes
      (middleware/wrap-defaults middleware/api-defaults)))

(comment
  (handler (mock/request :get "/new-game"))
  (handler (mock/request :get "/guess?guess=3"))
  (handler (mock/request :get "/dunno")))

(defonce server
  (run-jetty #'handler {:port 3001 :join? false}))

:core
