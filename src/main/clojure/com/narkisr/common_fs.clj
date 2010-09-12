(ns com.narkisr.common-fs
  (:import org.apache.commons.logging.LogFactory)
  (:use (clojure.contrib (seq-utils :only [find-first] ))))

(defn log-info [this text]
  (. (LogFactory/getLog (class this)) info text))

(defn log-warn [this text]
  (. (LogFactory/getLog (class this)) warn text))

(defn first-error [pre]
   (find-first (fn [[k v]] (not k)) pre))

(defmacro def-fs-fn
  ([name args] `(def-fs-fn ~name ~args {} (identity 0)))
  ([name args body] `(def-fs-fn ~name ~args {} ~body))
  ([name args pre body]
    (let [fn-name (clojure.lang.Symbol/intern (str "fs-" name))]
      `(defn ~fn-name ~(into ['this] args)
         (let [error# (first-error (:pre ~pre))]
          (if-not error# 
           (do #_(log-info 'this (str "calling - > " ~fn-name)) ~body (identity 0)) 
            (or (second error#) (:default ~pre) )))))))
