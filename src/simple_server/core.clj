(ns simple-server.core
  (:require [clojure.pprint]
            [ring.adapter.jetty :refer [run-jetty]]

            [ring.util.response :refer [response content-type set-cookie redirect not-found status]]
            [compojure.core :refer [GET POST ANY defroutes]]
            [compojure.coercions :refer [as-int]]
            [ring.middleware.defaults :as middleware]

            [ring.mock.request :as mock]
            [hiccup.core :refer [html]]
            
            [simple-server.simple-game :as game]
            [simple-server.database :as db]))

;; Initialize the database
(db/initalize-db)

(defn login-handler-html
  "Returns a login form"
  []
  (response
   (html
    [:body
     [:form {:method :post}
      [:h1 "Enter your username"]
      [:input {:type :text :name :username}]]])))

(defn successful-login-handler-html
  "Redirects the user to guess.html if there is a valid existing game,
   Otherwise redirects the user to create a new game"
  [username]
  (if (and (game/user-exists? username)
           (>= 4 (game/lookup-attempts username)))
    (-> (redirect "/guess.html")
        (set-cookie "auth" username))

    (-> (redirect "/new-game.html")
        (set-cookie "auth" username))))

(defn new-game-handler-html
  "Returns the starting game form"
  [username]
  (when (game/new-game! username)
    (response
     (html
      [:body
       [:h1 "Welcome to the guessing game!"]
       [:p "OK- start guessing " [:a {:href "/guess.html"} "here"]]]))))

(defn guess-handler-html
  "Returns a form containing the result of the guess made by the user"
  [guess username]
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
         :game-over [:div [:h2 "You won!"]
                     [:p "Start a new game " [:a {:href "/new-game.html"} "here"]]]
         :lose      [:div [:p "Out of attempts! Start again "
                           [:a {:href "/new-game.html"} "here"]]]
         :too-low   [:div [:h2 "Too low."]
                     [:p (str (- 5 (game/lookup-attempts username)) " attempts left")]
                     form]
         :too-high  [:div [:h2 "Too high."]
                     [:p (str (- 5 (game/lookup-attempts username)) " attempts left")]
                     form])]))))

(defn home-page-handler
  "Returns the home page"
  []
  (response "Welcome to the home page\nRefer to the README to get started"))

(defn ensure-auth-cookie
  "Ensures that the username cookie is available in all subsequent requests.
   If there is no username cookie then return a form requesting the user to login"
  [handler]
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

(defroutes login-routes
  (GET "/login.html"  []         (login-handler-html))
  (POST "/login.html" [username] (successful-login-handler-html username)))

(defroutes game-routes
  (GET "/new-game.html" {username :username} (new-game-handler-html username))
  (GET "/guess.html"    {username :username} (guess-handler-html nil username))
  (POST "/guess.html" [guess :as req]        (guess-handler-html guess (:username req))))

(defroutes html-routes
  login-routes
  (-> game-routes
      (ensure-auth-cookie)))

(def localhost-defaults (dissoc middleware/site-defaults :security))

(defroutes handler
  (-> html-routes
      (middleware/wrap-defaults localhost-defaults))
  (ANY "*"         []                (not-found "Sorry, No such URI on this server!")))

  ;; (handler (mock/request :get "/new-game"))
  ;; (handler (mock/request :get "/guess?guess=2"))
  ;; (handler (mock/request :get "/dunno"))

(defonce server
  (run-jetty #'handler {:port 3001 :join? false}))

:core
