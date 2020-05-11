(ns simple-server.database
  (:require [amazonica.aws.dynamodbv2 :as dynamo]))


(def cred {:access-key "aws-access-key"
           :secret-key "aws-secret-key"
           :endpoint   "http://localhost:4569"})

(dynamo/list-tables  cred)

; (dynamo/delete-table cred "Player")

(dynamo/create-table
 cred
 :table-name "Player"
 :key-schema              [{:attribute-name "name"   :key-type "HASH"}]
 :attribute-definitions   [{:attribute-name "name"    :attribute-type "S"}]
 :provisioned-throughput {:read-capacity-units 1 :write-capacity-units 1})

(dynamo/create-table
 cred
 :table-name "Game"
 :key-schema              [{:attribute-name "id"   :key-type "HASH"}
                           {:attribute-name "name" :key-type "RANGE"}]

 :attribute-definitions   [{:attribute-name "id" :attribute-type "S"}
                           {:attribute-name "name" :attribute-type "S"}]
 :provisioned-throughput {:read-capacity-units 1 :write-capacity-units 1})


(dynamo/put-item cred
                 :table-name "Player"
                 :return-consumed-capacity "TOTAL"
                 :return-item-collection-metrics "SIZE"
                 :item {
                        :name "Kenny Rogers"
                        :date (System/currentTimeMillis)
                        :high_score 77})



(dynamo/put-item cred
                 :table-name "Game"
                 :return-consumed-capacity "TOTAL"
                 :return-item-collection-metrics "SIZE"
                 :item {:id (str (java.util.UUID/randomUUID))
                        :name "Kenny Rogers"
                        :date (System/currentTimeMillis)
                        :details {:score 89
                                  :words ["Alpha" "Beta" "Gamma"]}})

(dynamo/query cred
       :table-name "Player"
       :limit 1
       :select "ALL_ATTRIBUTES"
       :key-conditions
       {:name {:attribute-value-list ["Kenny Rogers"] :comparison-operator "EQ"}})

(dynamo/query cred
              :table-name "Game"
              :limit 1
              :key-conditions
              {:name {:attribute-value-list ["Kenny Rogers"] :comparison-operator "EQ"}}
              )
