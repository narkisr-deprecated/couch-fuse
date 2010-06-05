(ns com.narkisr.fs-logic
  (:refer-clojure :exclude [partition])
  (:import java.io.File)
  (:use (clojure.contrib (def :only [defmacro-]) (str-utils2 :only [partition]))
    pattern-match))

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


(defn directory? [node] (= (type node) :directory))

(defn filehandle? [node] (= (type node) :filehandle))

;(defn lookup [path]
;  (if (= path "/") root
;    (let [f (java.io.File. path) parent (lookup (. f getParent))]
;      (if (directory? parent) (get-in parent [:files (. f getName)])))))

(defn lookup-keys [path]
  (match path
    ["/" dir "/" file] (list :files dir :files file)
    ["/" dir "/" & rest] (concat (list :files dir :files) (lookup-keys rest))
    [dir "/" & rest] (concat (list dir :files) (lookup-keys rest))
    ["/" file] (list :files file)
    ))

(defn lookup [path]
  (if (= path "/") root
    (get-in root (lookup-keys (rest (partition path #"/"))))))

(defn create-handle [metadata]
  (let [type-data {:type :filehandle}]
    (proxy [clojure.lang.IObj] []
      (withMeta [meta] (merge type-data metadata))
      (meta [] (merge type-data metadata))
      (toString [] (str "handle for " (:node metadata)))
      (finalize [] (println "finalizing")))))


;(lookup-keys (rest (partition "/bla/name/bl.txt" #"/")))
;(lookup-keys (rest (partition "/1077214558877334645/ae70b718342bc0d140743709e21cdbe6.jpeg" #"/")))
;(lookup-keys (rest (partition "/1077214558877334645" #"/")))
;(lookup-keys (rest (partition "/1077214558877334645/" #"/")))
;(lookup-keys (rest (partition "/" #"/")))

