(ns com.narkisr.fs-logic
  (:import java.io.File)
  (:use (clojure.contrib (def :only [defmacro-]) (seq-utils :only [flatten]))))

(defn with-type [type x]
  (with-meta x {:type type}))

(defmacro- def-fstype [name & keys]
  `(defstruct ~name :name :mode :xattrs ~@keys :lastmod))

(def-fstype directory :files)
(def-fstype file :content :size)
(def-fstype link :link)

(defmacro create-node [type & values]
  `(with-type ~(clojure.lang.Keyword/intern type)
    (struct ~type ~@values ~(/ (System/currentTimeMillis) 1000))))

(def root {}) ; must be binded when used to the actual root

;(defn lookup [path]
;  (if (= path "/") root
;    (let [f (java.io.File. path) parent (lookup (. f getParent))]
;      (if (= (type parent) :directory) (get-in parent [:files (. f getName)])))))

(defn lookup-keys [path]
  (-> (mapcat #(if (= % \/) [:files]) path) (vec) (merge (. (File. path) getName))))

(defn lookup [path]
  (get-in (lookup-keys path) root))

(defn directory? [node] (= (type node) :directory))

(defn filehandle? [node] (= (type node) :filehandle))

(defn create-handle [metadata]
  (let [type-data {:type :filehandle}]
    (proxy [clojure.lang.IObj] []
      (withMeta [meta] (merge type-data metadata))
      (meta [] (merge type-data metadata))
      (toString [] (str "handle for " (:node metadata)))
      (finalize [] (println "finalizing")))))


(lookup-keys "/bla/")
;
;(find-doc "combine" )
