(ns simple-server.core
  (:require [clojure.pprint])
  (:require [ring.adapter.jetty :refer [run-jetty]]))


;;; 1. What is a request?
;; Go to your browser and load this url: http://localhost:3001
;; You should see the message: "Hello, Class!" in your browser window.
;;
;; What is happening under the hood?
;; In a shell, run this command:
;; curl -v  http://localhost:3001
;; You should get an output which looks like this:
;; ===============================================
;; *   Trying 127.0.0.1...
;; * TCP_NODELAY set
;; * Expire in 200 ms for 4 (transfer 0x55c614eb35c0)
;; * Connected to localhost (127.0.0.1) port 3001 (#0)
;; > GET / HTTP/1.1
;; > Host: localhost:3001
;; > User-Agent: curl/7.64.0
;; > Accept: */*
;; >
;; < HTTP/1.1 200 OK
;; < Date: Fri, 08 May 2020 03:46:57 GMT
;; < Content-Type: text/plain
;; < Content-Length: 13
;; < Server: Jetty(9.2.21.v20170120)
;; <
;; * Connection #0 to host localhost left intact
;; Hello, class!%
;; ===============================================

(defn handler [request]
  (clojure.pprint/pprint request)
  (clojure.pprint/pprint (slurp (:body request)))
  {:status 200
   :headers {"Content-Type" "text/plain"}
   :body "Hello, class!"})

;; Look in your repl, it should have printed the request:

{:ssl-client-cert nil,
 :protocol "HTTP/1.1",
 :remote-addr "127.0.0.1",
 :headers {"user-agent" "curl/7.64.0", "accept" "*/*", "host" "localhost:3001"},
 :server-port 3001,
 :content-length nil,
 :content-type nil,
 :character-encoding nil,
 :uri "/",
 :server-name "localhost",
 :query-string nil,
 ;; :body  #object[org.eclipse.jetty.server.HttpInputOverHTTP 0xb54248c "HttpInputOverHTTP@b54248c"],
 :scheme :http,
 :request-method :get}

;; I have commented out the :body because it is not a readable object.
;; We will get back to what this contains later.

;; Here are the parameters we want to look at initially:
;;   :request-method
;;   :uri
;;
;; :request-method is the HTTP VERB being sent to the server.
;; Standard verbs include GET, POST, PUT and DELETE (and some others.)
;;
;; The default verb in the browser (and curl) is GET.
;;
;; Here is how you send a POST with curl:
;;    curl -v -X POST http://localhost:3001 -d foo=bar
;; (The only way to send a post from your browser is to submit a form, and
;;  we don't have a way to do that, yet!)
;;
;; Note how curl now reports:
;; > POST / HTTP/1.1
;; > Host: localhost:3001
;; > User-Agent: curl/7.64.0
;; > Accept: */*
;; > Content-Length: 7
;; > Content-Type: application/x-www-form-urlencoded
;; >
;; * upload completely sent off: 7 out of 7 bytes
;;
;; These extra length, the Content-Length: and Content-Type:
;; are to indicate that 7 bytes got send (or posted) to the
;; server, encoded in a standard web format.
;; foo=bar  contains 7 characters.

(defonce server
  (run-jetty #'handler {:port 3001 :join? false}))


:core
