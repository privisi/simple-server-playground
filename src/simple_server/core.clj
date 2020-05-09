(ns simple-server.core
  (:require [clojure.pprint]
            [ring.adapter.jetty :refer [run-jetty]]

            [ring.util.response :refer [response created redirect not-found status]]
            [compojure.core :refer [GET POST ANY defroutes]]

            [ring.mock.request :as mock]))


;;; Responses
;;;
;; Now that we've got some sensible routing going on,
;; let's look at our responses for a minute.
;;
;; We have a lot of code which returns maps which look like this:
{:status 200
 :headers {"Content-Type" "text/plain"}
 :body "OK- start guessing at /guess"}

;; ring.util.response provides some handy shortcuts for creating these maps; e.g.

;; The standard, everything was OK type response:
(response "Here is some text.")

;; 404 not found:
(not-found "Oops!")

;; With this in mind, let's rewrite some of our handlers.
;; Also, now that we understand that they ARE "handlers",
;; we'll rename them:

(def game-in-progress (atom nil))

(defn new-game-handler []
  ;; Make our new game:
  (reset! game-in-progress (+ 1 (rand-int 10)))
  (response "OK- start guessing at /guess"))

(defn extract-guess [qs]
  (and qs
       (Long. (second (clojure.string/split qs #"=")))))

(defn guess-handler [request]
  (if-let [guess (extract-guess (:query-string request))]
    (cond
      (= guess @game-in-progress)
      (and (reset! game-in-progress (+ 1 (rand-int 10)))
           (response  "Congratulations! You win!"))

      (< guess @game-in-progress)
      (response "Too low.")

      (> guess @game-in-progress)
      (response  "Too high."))

    ;; Notice, there is no helper for a 400 response, but we can
    ;; easily create one like this:
    (-> (response  "You need to supply a guess with /guess?guess=N")
        (status 400))))

(defroutes game-routes
  (GET "/new-game" [] (new-game-handler))  ; Why does one have it wrapped in (),
  (GET "/guess"    [] guess-handler)       ; and this one doesn't?
  (ANY "*"         [] (not-found "Sorry, No such URI on this server!")))      ; Our catch all, if nothing matches.

(game-routes  (mock/request :get "/new-game"))
(game-routes  (mock/request :get "/guess?guess=3"))
(game-routes  (mock/request :get "/dunno"))

(def handler game-routes)

(defonce server
  (run-jetty #'handler {:port 3001 :join? false}))


:core
