(ns holon.datomic.schema
  (:require [schema.core :as s]
            [schema.macros :as macros]
            [schema.spec.core :as spec]
            [schema.spec.leaf :as leaf]
            [schema.utils :as sutils])
  (:import [datomic.db DbId]))

(s/defschema DatomicDatom
  [(s/one datomic.db.DbId "entity id")
   (s/one s/Keyword "attr")
   (s/one s/Any "value")
   (s/one s/Inst "time")])

(s/defschema DatomicTX
  (s/either {:db/id s/Any
             s/Keyword s/Any}
            [(s/one s/Keyword "db/fn")
             (s/one datomic.db.DbId "id")
             (s/one s/Keyword "attr")
             (s/one s/Any "value")]))

(s/defschema DatomicTXReport
  {:db-before datomic.db.Db
   :db-after datomic.db.Db
   :tx-data [DatomicTX]})

(s/defschema DatomicSchema
  {:db/id DbId
   :db/ident s/Keyword
   :db/valueType (s/enum :db.type/keyword
                         :db.type/string
                         :db.type/boolean
                         :db.type/long
                         :db.type/float
                         :db.type/bigint
                         :db.type/double
                         :db.type/instant
                         :db.type/uuid
                         :db.type/uri)
   :db/cardinality (s/enum :db.cardinality/one
                           :db.cardinality/many)})

(s/defschema DatomicNorms
  {s/Keyword {:txes [[DatomicTX]]
              (s/optional-key :requires) [s/Keyword]}})

(s/defschema EntityLookup
  (s/either (s/named s/Num "entity id")
            [(s/one s/Keyword "attr")
             (s/one s/Any "unique value")]))

(defn entity?
  [e]
  (instance? datomic.Entity e))

(defrecord EntitySchema
    [schema]
  s/Schema
  (spec [this]
    (let [checker (s/checker schema)]
      (leaf/leaf-spec
       (fn [e]
         (cond
           (map? e) (checker e) ;; allow entities that have already been realized as maps
           (entity? e) (checker (into {} e))
           :else (macros/validation-error this e (list 'instance? 'Entity (sutils/value-name e))))))))
  (explain [this]
    (list 'entity 'Entity (or (:schema this)
                              (merge {} this)))))

(defn entity-schema?
  [schema]
  (= (type schema) EntitySchema))

(defn entity-schema
  [schema]
  (->EntitySchema schema))
