(ns simple-server.core
  (:require [clojure.pprint]
            [ring.adapter.jetty :refer [run-jetty]]

            [ring.util.response :refer [response created redirect not-found status]]
            [compojure.core :refer [GET POST ANY defroutes]]
            [ring.middleware.defaults :as middleware]

            [ring.mock.request :as mock]))


;;; Destructuring routes and parameters
;;; Let's see what else we can do with routes.
;;
;; Remember, we currently have this:

(comment
  (defroutes game-routes
    (GET "/new-game" [] (new-game-handler)) ; Why does one have it wrapped in (),
    (GET "/guess"    [] guess-handler)      ; and this one doesn't?
    (ANY "*"         [] (not-found "Sorry, No such URI on this server!"))))

;; What are the empty [], and why do we have (new-game-handler) vs guess-handler ?
;;
;; HTTP is often (usually?) used to transfer some specific information,
;; either in the form of query-strings, or post data.  What we had to do
;; with extract-guess is tedious, and we can tell we're going to need to
;; do this a LOT.  So compojure gives us some pretty powerful tools for this.


;;;; Destructuring the request object.

;; What is in the empty [] is actually a destructuring form which
;; gets applied to the (possibly modified) incoming request.

((GET "/foo" request {:status 200 :body request})
 (mock/request :get "/foo?name=Bob"))

;; We can apply the standard map destructuring constructs:
((GET "/foo" {headers :headers} {:status 200 :body headers})
 (mock/request :get "/foo?name=Bob"))

;; or
((GET "/foo" {:keys [server-name request-method uri]} {:status 200 :body [server-name request-method uri]})
 (mock/request :get "/foo?name=Bob"))



;;;; Extracting parts of the URI

(defn greet-user-handler [name]
  (response (format "Greetings, %s!" name)))

((GET "/foo/:name" [name] (greet-user-handler name))
 (mock/request :get "/foo/Sally"))

;; Compojure, if it sees a vector as the destructuring construct,
;; takes things one step further, and does some magic behind the
;; scenes; in this case, binds any keyword part in the URI path
;; to a symbol of the same name.

;; This is nice, because now we can see how we can start separating
;; our functions which do logic from those that do "http stuff";
;; in particular, we could rewrite above like this:

(defn greet-user [name]
  (format "Greetings, %s!" name))

((GET "/foo/:name" [name] (response (greet-user name)))
 (mock/request :get "/foo/Sally"))

;; And, it turns out, if we just return a string, Compojure will interpret
;; that as a success response, so we can actually just write this¹:

((GET "/foo/:name" [name] (greet-user name))
 (mock/request :get "/foo/Sally"))


;; 2. extracting query string parameters

;; It would be nice if we could get the data from the query string
;; or post data in a similar way, i.e. write:


;; Remember, we were trying to get this to work:

((GET "/foo" [name] {:status 200 :body (str "The name is: " name)})
 (mock/request :get "/foo?name=Bob"))


;; Look at this:
((middleware/wrap-defaults (GET "/foo" [name] {:status 200 :body (str "The name is: " name)}) middleware/api-defaults)
 (mock/request :get "/foo?name=Bob"))

;; Whoa!  It works!  Hard to read though.  What's really going on here?

(def example-route (GET "/foo" [name] {:status 200 :body (str "The name is: " name)}))

(def wrapped-example-route (-> example-route
                               (middleware/wrap-defaults middleware/api-defaults)))

(wrapped-example-route (mock/request :get "/foo?name=Bob"))

;; This is an instance of a very common pattern in functional programming style;
;; adapting, or WRAPPING, one function with another.
;; This way, some code gets to run /in the middle/ between your code and the
;; external world, so we call this "middle"-ware.

;; To understand this, let's write some simple middlewares of our own.

;; All handlers have this form:

(defn some-handler [request]
  ;; ....
  {:status 200
   :headers {}
   :body "Some response body"})

;; i.e. a handler is a function which transforms a REQUEST into a RESPONSE.

;; Suppose we wanted to always add a custom header to every outgoing response,
;; for example, this often used response header, which controls caching on
;; browsers and proxies:

(-> (some-handler (mock/request :get "/foobar"))
    (assoc-in [:headers "Cache-Control"] "max-age=3600"))

;; It'd be a real pain to have to modify each one of our handlers to do this.
;; Consider this function:

(defn cache-control-middleware [handler]
  (fn [request]
    (println "Running the cache-control-middleware")
    (-> (handler request)
        (assoc-in [:headers "Cache-Control"] "max-age=3600"))))

;; This is a middleware (higher order) function: It accepts a handler FUNCTION,
;; and returns a MODIFIED handler function, which calls the original handler,
;; and performs some extra work on the response returned by the original handler.

;; I call these "outbound" middlewares, because they modify the result of the original
;; handler.

;; Here is another example:
(defn parse-cookies [request] "some dummy cookie.")

(defn cookie-parsing-middleware [handler]
  (fn [request]
    (println "Running the cookie-parsing-middleware")
    (handler (-> request
                 (assoc :cookies (parse-cookies request))))))

;; This makes available to the original handler a new field, :cookies
;; which it can use if it needs to to compute its response.  All together, we have:

(def some-wrapped-handler
  (-> (ANY "*" [] (fn [request]
                    (response (str "Responding, having seen: " (:cookies request)))))
      (cache-control-middleware)
      (cookie-parsing-middleware)))

(some-wrapped-handler (mock/request :get "/foobar"))
;; Note the order in which the middlewares ran!
;;
;; In this case order doesn't matter, but sometimes, one middleware may depend
;; on context established by another one, so it's good to remember that
;; they run "last one first".


;; So now we understand our original code (rewritten slightly):
;; The (middleware/wrap-defaults  middleware/api-defaults) wraps
;; the incoming request, parsing the query string so that the GET
;; handler knows to bind `name` from the query string, if present.

((-> (GET "/foo" [name] {:status 200 :body (str "The name is: " name)})
     (middleware/wrap-defaults  middleware/api-defaults))
 (mock/request :get "/foo?name=Bob"))


;;;; Nothing changed below.

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

;; ¹ Well, almost.  You may note the :headers have changed.
;;   We'll discuss how to best modify them soon, when we talk
;;   about /middleware/.
:core
