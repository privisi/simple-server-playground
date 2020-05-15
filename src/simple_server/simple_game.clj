(ns simple-server.simple-game)


(def games-in-progress (atom {}))

;; Imagine this:
(defn make-new-game []
  {:attempts 0
   :target (+ 1 (rand-int 10))})

(defn lookup-target [user]
  (:target (@games-in-progress user)))

(defn new-game! [user]
  ;; Make our new game:
  (swap! games-in-progress assoc user (make-new-game))
  :ok)

(defn guess-answer [guess user]
  (cond
    (nil? guess) nil

    (= guess (lookup-target user))
    (and (swap! games-in-progress dissoc user)
         :game-over)

    (< guess (lookup-target user))
    :too-low

    (> guess (lookup-target user))
    :too-high))

