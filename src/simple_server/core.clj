(ns simple-server.core
  (:require [clojure.pprint]
            [ring.adapter.jetty :refer [run-jetty]]

            [ring.util.response :refer [response created redirect not-found status]]
            [compojure.core :refer [GET POST ANY defroutes context]]
            [compojure.coercions :refer [as-int]]
            [ring.middleware.defaults :as middleware]

            [hiccup.core :refer [html]]
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
  (GET "/guess"    [guess :<< as-int] (guess-handler guess)))


(defn new-game-handler-html []
  (when (game/new-game!)
    (response
     (html
      [:body
       [:h1 "Welcome to the guessing game!"]
       [:p "OK- start guessing " [:a {:href "/guess.html"} "here"]]]))))

(defn guess-handler-html [guess]
  (println "They are guessing " guess)
  (let [form [:div
              [:h1 "Enter your guess"]
              [:form {:method :post}
               [:input {:type :text :name :guess}]]]
        guess (as-int guess)]
    (response
     (html
      [:body
       (condp = (game/guess-answer guess)
         nil        form
         :game-over [:h2 "You won!"]
         :too-low   [:div [:h2 "Too low."] form]
         :too-high  [:div [:h2 "Too high."] form])]))))

(defroutes html-game-routes
  (GET "/new-game.html" [] (new-game-handler-html))
  (GET "/guess.html"    [] (guess-handler-html nil))
  (POST "/guess.html"   [guess] (guess-handler-html guess)))

(defroutes handler
  (context "/api" []
    (-> game-routes
        (middleware/wrap-defaults middleware/api-defaults)))
  (context "" []
    (-> html-game-routes
        (middleware/wrap-defaults (dissoc middleware/site-defaults :security))))
  (ANY "*"         []                 (not-found "Sorry, No such URI on this server!")))


(comment
  (handler (mock/request :get "/new-game"))
  (handler (mock/request :get "/new-game-v2.html"))
  (handler (mock/request :get "/guess?guess=3"))
  (handler (mock/request :get "/dunno")))

(defonce server
  (run-jetty #'handler {:port 3001 :join? false}))

:core
