(ns simple-server.core
  (:require [clojure.pprint]
            [ring.adapter.jetty :refer [run-jetty]]

            [ring.util.response :refer [response content-type created redirect not-found status]]
            [compojure.core :refer [GET POST ANY defroutes]]
            [compojure.coercions :refer [as-int]]
            [ring.middleware.defaults :as middleware]

            [ring.mock.request :as mock]
            [simple-server.simple-game :as game]))

(def active-logins (atom #{}))

(defn web-content
  "Returns a handler for web page display"
  [text]
  (-> text
      (response)
      (content-type "text/plain")))

(defn logged-in?
  "Returns if ACCOUNT is logged in (exists)"
  [account]
  (some #{account} @active-logins))

(defn new-game-handler 
  "Starts a new game on ACCOUNT"
  [account]
  (if (logged-in? account)
    (when (game/new-game! account)
      (web-content (str "OK- start guessing at /" account "/guess?guess=N")))
    (-> (web-content (str "Account " account " does not exist!\nPlease login at /login?name=NAME"))
        (status 400))))

(defn guess-handler 
  "Returns the outcome of the GUESS for ACCOUNT"
  [account guess]
  (if (logged-in? account)
    (condp = (game/guess-answer account guess)
      nil       (-> (web-content  "You need to supply a guess with /guess?guess=N")
                    (status 400))
      :win (web-content  "Congratulations! You win!")
      :too-low   (web-content (str "Too low. " (- 5 (@game/guesses account)) " guesses left."))
      :too-high  (web-content  (str "Too High. " (- 5 (@game/guesses account)) " guesses left."))
      :lose (web-content "Too many guesses, try again"))
    (-> (web-content (str "Account " account " does not exist!\nPlease login at /login?name=NAME"))
        (status 400))))

(defn login-handler
  "Adds player-name to active-logins and redirects to accounts home screen after login"
  [player-name]
  (swap! active-logins conj player-name)
  (redirect (str "/" player-name)))

(defn account-handler 
  "Returns the home page of ACCOUNT if logged in.
   Otherwise returns a 404 not found"
  [account]
  (if (logged-in? account)
    (web-content (str "Welcome " account ". You are logged in!\n"
                      "To start a new game: /" account "/new-game"))
    (not-found "Sorry, No such URI on this server!")))

(defn home-page-handler
  "Returns the home page"
  []
  (web-content "Welcome to the home page\nRefer to the README to get started"))

(defroutes game-routes
  (GET "/login"    [name]             (login-handler name))
  (GET "/:account/new-game" [account] (new-game-handler account))
  (GET "/:account/guess"    [account guess :<< as-int] (guess-handler account guess))
  (GET "/:account" [account]          (account-handler account))
  (GET "/" []                        (home-page-handler))
  (GET "*"         []                 (not-found "Sorry, No such URI on this server!")))

(def handler
  (-> game-routes
      (middleware/wrap-defaults middleware/api-defaults)))

  ;; (handler (mock/request :get "/new-game"))
  ;; (handler (mock/request :get "/guess?guess=2"))
  ;; (handler (mock/request :get "/dunno"))

(defonce server
  (run-jetty #'handler {:port 3001 :join? false}))

:core
