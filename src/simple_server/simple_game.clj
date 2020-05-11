(ns simple-server.simple-game)


(def games-in-progress (atom {}))
(def guesses (atom {}))

(defn new-game! [account]
  ;; Make our new game:
  (swap! games-in-progress assoc account (+ 1 (rand-int 10)))
  (swap! guesses assoc account 0)
  :ok)

(defn guess-answer [account guess]
  (cond
    (nil? guess) nil

    (= guess (@games-in-progress account))
    (do (swap! games-in-progress assoc account (+ 1 (rand-int 10)))
        (swap! guesses assoc account 0)
        :win)
    
    (>= (@guesses account) 4)
    (do (swap! games-in-progress assoc account (+ 1 (rand-int 10)))
        (swap! guesses assoc account 0)
        :lose)

    (< guess (@games-in-progress account))
    (do (swap! guesses assoc account (inc (@guesses account)))
        :too-low)

    (> guess (@games-in-progress account))
    (do (swap! guesses assoc account (inc (@guesses account)))
        :too-high)))
