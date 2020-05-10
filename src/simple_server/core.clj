(ns simple-server.core
  (:require [clojure.pprint]
            [ring.adapter.jetty :refer [run-jetty]]

            [ring.util.response :refer [response created redirect not-found status]]
            [compojure.core :refer [GET POST ANY defroutes]]
            [compojure.coercions :refer [as-int]]
            [ring.middleware.defaults :as middleware]

            [ring.mock.request :as mock]
            [simple-server.simple-game :as game]))

;;; Finally, let us truly separate concerns between our "application code"
;;; and our "http code".  Our game now lives in its own namespace, and
;;; is fully testable independent of our "presentation layer".

(defn new-game-handler []
  (when (game/new-game!)
    (response "OK- start guessing at /guess")))

(defn guess-handler [guess]
  (condp = (game/guess-answer guess)
    nil       (-> (response  "You need to supply a guess with /guess?guess=N")
                  (status 400))
    :game-over (response  "Congratulations! You win!")
    :too-low   (response "Too low.")
    :too-high  (response  "Too high.")))

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
