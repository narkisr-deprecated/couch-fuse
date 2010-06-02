(ns com.narkisr.common-fs
  (:import org.apache.commons.logging.LogFactory))

(defn log-access [this name args] (. (LogFactory/getLog (class this)) debug (str name args)))

(defmacro def-fs-fn
  ([name args] `(def-fs-fn ~name ~args true 0 (identity 0)))
  ([name args body] `(def-fs-fn ~name ~args true 0 ~body))
  ([name args pre error] `(def-fs-fn ~name ~args ~pre ~error (identity 0)))
  ([name args pre error body]
    (let [fn-name (clojure.lang.Symbol/intern (str "fs-" name))]
    `(defn ~fn-name ~(into ['this] args)
      (if ~pre (do (log-access 'this ~fn-name ~args) ~body (identity 0)) ~error)))))


