(ns gel.emitter
  (:require [clojure.java.io :as io]
            [gel.parser :as p]))

(defn get*
  "Calls get if given a single key, or get-in if given a sequential
  collection."
  ([m k-or-ks]
     (get* m k-or-ks nil))
  ([m k-or-ks not-found]
     ((if (sequential? k-or-ks) get-in get) m k-or-ks not-found)))

(defn lookup
  "Gets a value from the given stack of maps, successively checking
  each one until a value is found. Returns nil if the value is nil or
  if the key doesn't exist in any of the maps."
  [stack k] ;; have a not found value?
  (when-let [top (first stack)]
    (if-let [v (get* top k ::sentinel)]
      (if (= ::sentinel v)
        (recur (rest stack) k)
        v))))

(def ops
  {"==" =
   "!=" not=
   ">" >
   "<" <
   ">=" >=
   "<=" <=
   "or" (fn [lval rval] (or lval rval))
   "and" (fn [lval rval] (and lval rval))
   ;; todo: "contains"
   })

(defmulti emit-node (fn [ast env buf]
                      (:type ast)))

(defmulti emit-tag (fn [ast env buf]
                     (:tag ast)))

(defn emit-body
  [ast env buf]
  (loop [env env body ast]
    (if-let [node (first body)]
      (recur (emit-node node env buf) (next body))
      env)))

(defmethod emit-node :document
  [ast env buf]
  (emit-body (:body ast) env buf))

(defmethod emit-node :output
  [ast env buf]
  (.append buf (lookup env (:key ast)))
  ;; todo: apply filters
  env)

(defmethod emit-node :tag
  [ast env buf]
  (emit-tag ast env buf))

(defmethod emit-node :content
  [ast env buf]
  (.append buf (:value ast))
  env)

(defmethod emit-tag :comment
  [ast env buf]
  env)

(defmethod emit-tag :if
  [ast env buf]
  (let [{:keys [lval op rval then]} ast]
    (if (= op "fn")
      (if ((comp not nil?) (lookup env lval))
        (emit-body then env buf)
        env)
      (if-let [op-fn (get ops op)]
        (if (op-fn (lookup env lval) (if (vector? rval) (lookup env rval) rval))
          (emit-body then env buf)
          env)
        env)))) ;; todo: error due to unsupported op
;; todo: support elsif* and else

(defmethod emit-tag :for
  [ast env buf]
  (let []
    (loop [env env coll (lookup env (:coll ast))]
      (if-let [item (first coll)]
        (recur (next (emit-body (:body ast)
                                (cons {(:var ast) item} env)
                                buf))
               (next coll))
        env))))

(defmethod emit-tag :include
  [ast env buf]
  (let [incl-env (if (contains? ast :params)
                   (cons {:include (:params ast)} env)
                   (cons {} env))
        file-name (when-let [file-name (:file-name ast)]
                    (if (string? file-name)
                      file-name
                      (lookup incl-env (get-in ast [:file-name :key])))) ;; apply filters
        incl ((lookup env :gel.core/include-fn) file-name)]
    (next (emit-node (p/parse incl)
                     (cons (first incl-env) (next incl-env))
                     buf))))

(defmethod emit-tag :capture
  [ast env buf]
  (let [capture-buf (StringBuilder.)
        capture-env (emit-body (:body ast) env capture-buf)]
    (let [env2 (-> (first capture-env)
                   (assoc (keyword (:var ast)) (str capture-buf))
                   (cons (next capture-env)))]
      env2)))

(defmethod emit-tag :custom
  [ast env buf]
  (if-let [f (lookup env [:gel.core/custom-tags (:custom-type ast)])]
    (f ast env buf)
    env)) ;; todo: error?

(defn emit
  [ast env buf]
  (emit-node ast (list env) buf))
