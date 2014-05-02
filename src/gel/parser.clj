(ns gel.parser
  (:require [clojure.java.io :as io]
            [instaparse.core :as insta]
            [instaparse.failure :as instf]))

(defn ^:private tag
  [tag style kv]
  (assoc kv
    :type :tag
    :tag tag
    :style style))

(def transformers
  {:document
   (fn [block]
     {:type :document
      :body block})

   :block
   (fn [& block-nodes]
     (vec block-nodes))

   :tag
   (fn [tag]
     (assoc tag :type :tag))

   :var keyword

   :quoted-var identity

   :dotted-var
   (fn [& vars]
     (vec (map keyword vars)))

   :literal identity

   :param
   (fn [key val]
     {key val})

   :op identity

   :output
   (fn [key & filters]
     {:type :output
      :key key
      :filters (vec filters)})

   :content
   (fn [text]
     {:type :content
      :value text})

   :comment
   (fn [body]
     {:tag :comment
      :body body
      :style :block})

   :include
   (fn include
     ([literal]
        (tag :include :inline {:file-name literal}))
     ([literal & params]
        (let [base (include literal)]
          (if (map? (first params))
            (assoc base :params (apply merge params))
            (if (next params)
              (assoc base
                :with (first params)
                :params (apply merge (next params)))
              (assoc base
                :with (first params)))))))

   :if
   (fn
     ([lval then]
        (tag :if :block {:lval lval :op "fn" :then then}))
     ([lval op rval then]
        (tag :if :block {:lval lval :op op :rval rval :then then})))

   :for
   (fn [var coll body]
     (tag :for :block {:var var :coll coll :body body}))

   :capture
   (fn [var body]
     (tag :capture :block {:var var :body body}))

   :custom
   (fn
     ([name opts]
        (tag :custom :inline {:opts opts :custom-type (keyword name)}))
     ([name opts body]
        (tag :custom :block {:opts opts :custom-type (keyword name) :body body})))

   :custom-opts
   (fn [& opts]
     (vec opts))

   })

(def parser (insta/parser (io/resource "liquid.bnf")))

(defn parse
  [text]
  (let [result (insta/parse parser text)]
    (if (insta/failure? result)
      (instf/pprint-failure result) ;; todo: deal with errors better
      (insta/transform transformers result))))
