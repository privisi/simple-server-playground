(ns simple-server.database
  (:require [amazonica.aws.dynamodbv2 :as dynamo]
            [cheshire.core]
            [camel-snake-kebab.core :refer [->kebab-case-keyword]]
            [clojure.java.io :as io]))

(def cred {:access-key "aws-access-key"
           :secret-key "aws-secret-key"
           :endpoint   "http://localhost:4569"})

(dynamo/list-tables  cred)

(dynamo/delete-table cred "battle-royale")

(dynamo/create-table
 cred
 :table-name "battle-royale"
 :attribute-definitions   [{:attribute-name "pk"    :attribute-type "S"}  ; Primary key
                           {:attribute-name "sk"    :attribute-type "S"}] ; Secondary key
 :key-schema              [{:attribute-name "pk"   :key-type "HASH"}
                           {:attribute-name "sk"   :key-type "RANGE"}]
 :provisioned-throughput {:read-capacity-units 10 :write-capacity-units 10})

(dynamo/describe-table cred "battle-royale")

(def items
  (with-open [rdr (io/reader "/home/ap/Consulting/clients/gojee/work/simple-server/dynamo/scripts/items.json")]
    (doall
     (->> (line-seq rdr)
          (map #(cheshire.core/parse-string % ->kebab-case-keyword ))))))

(defn make-put-request [item]
  {:put-request {:item item}})


(doseq [chunk (partition 10 items)]
  (println "Doing chunk...")
  (dynamo/batch-write-item
   cred
   :return-consumed-capacity "TOTAL"
   :return-item-collection-metrics "SIZE"
   :request-items
   {"battle-royale" (mapv make-put-request chunk)}))



(def game-id "3d4285f0-e52b-401a-a59b-112b38c4a26b")


(take 5 (:items (dynamo/scan cred :table-name "battle-royale")))

({:address "6747 Rebecca Course\nWest Timothy, MS 18097", :birthdate "1909-03-10", :sk "#METADATA#jennifer32", :name "Gloria Thomas", :pk "USER#jennifer32", :email "mary34@gmail.com", :username "jennifer32"}
 {:address "Unit 8929 Box 5827\nDPO AE 25338", :birthdate "1932-12-28", :sk "#METADATA#thomas79", :name "Timothy Dawson", :pk "USER#thomas79", :email "kimberlyduncan@yahoo.com", :username "thomas79"}
 {:address "0371 Mark Brook Suite 485\nNew Jacobshire, NH 97264", :birthdate "1905-02-09", :sk "#METADATA#brandon30", :name "James Davis", :pk "USER#brandon30", :email "theresaschwartz@gmail.com", :username "brandon30"})

;; Fetches everything related to `game-id`
(dynamo/query cred
              :table-name "battle-royale"
              :select "ALL_ATTRIBUTES"
              :key-conditions
              {:pk {:attribute-value-list [(str "GAME#" game-id)] :comparison-operator "EQ"}})

;; Restricts to only those rows where metadata looks like users
(->> (dynamo/query cred
                   :table-name "battle-royale"
                   :key-condition-expression "pk = :pk AND sk BETWEEN :metadata AND :users"
                   :expression-attribute-values {":pk"       {:s (str "GAME#" game-id)}
                                                 ":metadata" {:s (str "METADATA#" game-id)}
                                                 ":users"    {:s "USER$"}})
     :items
     (take 2))

;; Upshot: the syntax of this stuff is horrible, and if you don't
;; design everything in advance just so you're totally hosed on
;; ad-hoc queries.


(dynamo/put-item cred
                 :table-name "battle-royale"
                 :item {:address "Unit 8929 Box 5827\nDPO AE 25338", :birthdate "1832-12-28",
                        :sk "#METADATA#alain01",
                        :pk "USER#alain01",
                        :name "Alain Picard",
                        :email "kimberlyduncan@yahoo.com", :username "alain"
                        :a-set #{1 2 3}
                        :a-map {:quux "foo" ::bar "The Basement"}
                        :nested {::quux #{1 2 3}
                                 :high-score 77
                                 :date (str (java.util.Date.))}})

(dynamo/get-item cred
                 :table-name "battle-royale"
                 :key {:pk {:s "USER#alain01"}
                       :sk {:s "#METADATA#alain01"}})
