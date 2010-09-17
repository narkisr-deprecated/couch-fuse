(ns com.narkisr.common-fs
  (:import org.apache.commons.logging.LogFactory)
  (:use (clojure.contrib (seq-utils :only [find-first]) (pprint))))

(defn log-info [this text]
  (. (LogFactory/getLog (class this)) info text))

(defn log-warn [this text]
  (. (LogFactory/getLog (class this)) warn text))

(defn forms [pre]
  (map (fn [[k v]] k) pre))

(defn first-error [pre]
  (find-first (fn [[[cond errno] form]] (not cond)) pre))

(defmacro def-fs-fn
  ([name args] `(def-fs-fn ~name ~args {} (identity 0)))
  ([name args body] `(def-fs-fn ~name ~args {} ~body))
  ([name args pre body]
    (let [fn-name (clojure.lang.Symbol/intern (str "fs-" name))]
      `(defn ~fn-name ~(into ['this] args)
         (let [forms# '~(-> pre :pre forms) error# (first-error (map vector (:pre ~pre) forms#))]
           (if-not error# 
            (do (log-info 'this (str "calling - > " ~fn-name)) ~body (identity 0)) 
            (let [cond-form# (error# 1) code# (or (get-in error# [0 1]) (:default ~pre))] 
              (log-warn 'this (str "pre condition " cond-form# " failed with code " code#)) code#)))))))


#_(pprint (macroexpand-1 '(def-fs-fn bla [path mode] {:pre [[(nil? path) -1] [(pos? mode) -2] ]} (println "done"))))
