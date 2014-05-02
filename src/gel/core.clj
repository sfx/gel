(ns gel.core
  (:require [clojure.java.io :as io]
            [gel.emitter :as e]
            [gel.parser :as p]))

(def default-env
  {::include-fn
   (fn [file-name]
     (slurp (io/resource file-name)))})

(defn render
  ([text]
     (render text {}))
  ([text env]
     (if-let [ast (p/parse text)]
       (let [buf (StringBuilder.)]
         (e/emit ast (merge default-env env) buf)
         (str buf))
       "")))
