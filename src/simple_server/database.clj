(ns simple-server.database
  (:require [amazonica.aws.dynamodbv2 :as dynamo]
            [cheshire.core]
            [camel-snake-kebab.core :refer [->kebab-case-keyword]]
            [clojure.java.io :as io]))

;; The Amazon local dynamoDb needs the access keys generated in NoSQL
(def cred {:access-key "1xhobe"
           :secret-key "5n273p"
           :endpoint   "http://localhost:8000"})

; (dynamo/delete-table cred "guessing-game")

(defn initalize-db
  "If there is no table, create it."
  []
  (if (empty? (dynamo/list-tables cred))
    (do
      (println "No database detected\nCreating database")
      (dynamo/create-table
       cred
       :table-name "guessing-game"
       :attribute-definitions   [{:attribute-name "pk"    :attribute-type "S"}  ; Primary key
                                 {:attribute-name "sk"    :attribute-type "S"}] ; Secondary key
       :key-schema              [{:attribute-name "pk"   :key-type "HASH"}
                                 {:attribute-name "sk"   :key-type "RANGE"}]
       :provisioned-throughput {:read-capacity-units 10 :write-capacity-units 10}))

    (println "The database already exists")))

(defn make-put-request
  "Puts a new item into the guessing-game table"
  [username target]
  (dynamo/put-item cred
                   :table-name "guessing-game"
                   :item {:pk (str "USER#" username)
                          :sk (str "#METADATA#" username)
                          :target target
                          :guess nil
                          :attempts 0}))

(defn make-update-request
  "updates an existing item in the guessing-game table"
  [username item-name new-value]
  (dynamo/update-item cred
                      :table-name "guessing-game"
                      :key {:pk (str "USER#" username)
                            :sk (str "#METADATA#" username)}
                      :update-expression "SET #item = :newval"
                      :expression-attribute-names {"#item" item-name}
                      :expression-attribute-values {":newval" new-value}))

(defn make-get-request
  "Gets an existing item from the guessing-game table"
  [username item-key]
  (get-in (dynamo/get-item cred
                           :table-name "guessing-game"
                           :key {:pk (str "USER#" username)
                                 :sk (str "#METADATA#" username)})
          [:item item-key]))