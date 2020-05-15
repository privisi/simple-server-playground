(ns simple-server.simple-game
  (:require [simple-server.database :as db]))

(defn user-exists?
  "Returns if the USER exists in the database"
  [user]
  (db/make-get-request user :pk))

(defn lookup-target
  "Returns the target value for USER"
  [user]
  (db/make-get-request user :target))

(defn lookup-attempts
  "Returns how many attempts the USER has made"
  [user]
  (db/make-get-request user :attempts))

(defn new-game!
  "Creates a new game in the database.
   If the user exists, update the corresponding fields,
   Otherwise create a new item in the database"
  [user]
  (if (user-exists? user)
    (do
      (db/make-update-request user "target" (+ 1 (rand-int 10)))
      (db/make-update-request user "attempts" 0))
    (db/make-put-request user (+ 1 (rand-int 10))))
  :ok)

(defn guess-answer
  "Returns the result of the GUESS made by the USER"
  [guess user]
  (cond
    (nil? guess) nil

    (= guess (lookup-target user))
    :game-over

    (>= (lookup-attempts user) 4)
    (do (db/make-update-request user "attempts" 5)
        :lose)

    (< guess (lookup-target user))
    (do (db/make-update-request user "attempts" (inc (lookup-attempts user)))
        :too-low)

    (> guess (lookup-target user))
    (do (db/make-update-request user "attempts" (inc (lookup-attempts user)))
        :too-high)))