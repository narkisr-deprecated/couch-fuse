(ns com.narkisr.common-fs
  (:import org.apache.commons.logging.LogFactory))

(defn log-info [this text]
  (. (LogFactory/getLog (class this)) info text))

(defn log-warn [this text]
  (. (LogFactory/getLog (class this)) warn text))

(defn log-debug [this text]
  (. (LogFactory/getLog (class this)) debug text))

(defn forms [pre]
  (map (fn [[k v]] [k v]) pre))

(defn first-error [pre]
  (first (filter  (fn [[[cond errno] form]] (not cond)) pre)))

(defmacro def-fs-fn
  ([name args] `(def-fs-fn ~name ~args {} (identity 0)))
  ([name args body] `(def-fs-fn ~name ~args {} ~body))
  ([name args pre body]
    (let [fn-name (clojure.lang.Symbol/intern (str "fs-" name))]
      `(defn ~fn-name ~(into ['this] args)
         (let [error# (first-error (map vector (:pre ~pre) '~(:pre pre)))]
           (if-not error# 
            (do 
              (log-debug 'this (str "calling - > " ~fn-name)) ~body (identity 0)) 
            (do
              (log-warn 'this 
                (str 
                  "pre condition " (get-in error# [1 0])  " on " '~name 
                  " failed with code " (get-in error# [1 1])
                  " for args " ~args))
              (or (get-in error# [0 1]) (:default ~pre)))))))))


#_(pprint (macroexpand-1 '(def-fs-fn bla [path mode] {:pre [[(nil? path) -1] [(pos? mode) -2] ]} (println "done"))))
