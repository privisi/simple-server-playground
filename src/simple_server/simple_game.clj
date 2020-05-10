(ns simple-server.simple-game)


(def game-in-progress (atom nil))

(defn new-game! []
  ;; Make our new game:
  (reset! game-in-progress (+ 1 (rand-int 10)))
  :ok)

(defn guess-answer [guess]
  (cond
    (nil? guess) nil

    (= guess @game-in-progress)
    (and (reset! game-in-progress (+ 1 (rand-int 10)))
         :game-over)

    (< guess @game-in-progress)
    :too-low

    (> guess @game-in-progress)
    :too-high))
