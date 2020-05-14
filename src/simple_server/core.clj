(ns simple-server.core
  (:require [clojure.pprint]
            [ring.adapter.jetty :refer [run-jetty]]

            [ring.util.response :refer [response created redirect not-found status set-cookie]]
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


;;;; HTML, form based implementation

;; This url is "public", i.e. doesn't need a cookie.
(defn login-handler-html []
  (response
   (html
    [:body
     [:form {:method :post}
      [:h1 "Enter your username" ]
      [:input {:type :text :name :username}]]])))

;; When the form POSTs back to itself, we set the cookie,
;; and redirect to the new game.
(defn successful-login-handler-html [username]
  (-> (redirect "/new-game.html")
      (set-cookie "auth" username)))

(defroutes login-routes
  (GET "/login.html" [] (login-handler-html))
  ;; Note how username corresponds to the :name of the input field.
  (POST "/login.html" [username] (successful-login-handler-html username)))


;; Now rewrite our game handlers to show some html:
(defn new-game-handler-html [username]
  (when (game/new-game! username)
    (response
     (html
      [:body
       [:h1 "Welcome to the guessing game!"]
       [:p "OK- start guessing " [:a {:href "/guess.html"} "here"]]]))))

(defn guess-handler-html [guess username]
  (let [form [:div
              [:h1 "Enter your guess"]
              [:form {:method :post}
               [:input {:type :text :name :guess}]]]
        guess (as-int guess)]
    (response
     (html
      [:body
       (condp = (game/guess-answer guess username)
         nil        form
         :game-over [:h2 "You won!"]
         :too-low   [:div [:h2 "Too low."] form]
         :too-high  [:div [:h2 "Too high."] form])]))))

(defroutes html-game-routes
  (GET "/new-game.html" {username :username} (new-game-handler-html username))
  (GET "/guess.html"    {username :username} (guess-handler-html nil username))
  (POST "/guess.html"   [guess :as req] (guess-handler-html guess (:username req))))

;; We want all the game playing URLS (i.e. the html-game-routes) to
;; be protected against unauthorized use.
;; So we write some middleware to ensure the cookie is there:

(defn ensure-auth-cookie [handler]
  (fn [request]
    (if-let [cookie (get-in request [:cookies "auth"])] ; We don't care about the value, just its presence
      (handler (-> request
                   (assoc :username (:value cookie))))
      {:status  403                     ; Unauthorized.
       :headers {"Content-Type" "text/html"}
       :body    (html
                 [:body
                  [:h1 "You must login first."]
                  [:a {:href "/login.html"}  "Login here."]])})))

;; Now wrap our new middleware around our route
(defroutes html-routes
  login-routes                          ; Public URLs
  (-> html-game-routes                  ; Protected URLs
      (ensure-auth-cookie)))

(def localhost-defaults (dissoc middleware/site-defaults :security)) ; To get around CRSF nonsense.  Don't do this in production.

(defroutes handler
  (context "/api" []
    (-> game-routes
        (middleware/wrap-defaults middleware/api-defaults)))
  (-> html-routes
      (middleware/wrap-defaults localhost-defaults))
  (ANY "*"         []                 (not-found "Sorry, No such URI on this server!")))


(defn test-guess [guess]
  ;; Creating a request with the required cookie is slightly more
  ;; involved, and looks like this:
  (handler
   (->
    (mock/request :post "/guess.html" {:guess guess})
    (assoc-in [:headers "cookie"] "ring-session=d5e8255b-eb1d-405b-9ede-bae6e4d0d40c; auth=alain"))))

(comment
  (handler (mock/request :get "/login.html"))
  (handler (mock/request :post "/login.html" {:username "Alain"}))
  (handler (mock/request :post "/guess.html" {:guess "3"}))
  (handler (mock/request :get "/new-game"))
  (handler (mock/request :get "/new-game-v2.html"))
  (handler (mock/request :get "/api/guess?guess=3"))
  (handler (mock/request :get "/dunno")))

(defonce server
  (run-jetty #'handler {:port 3001 :join? false}))

:core




