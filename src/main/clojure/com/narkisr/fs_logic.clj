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

(def root (ref {})) ; must be binded when used to the actual root

(defn directory? [node] (= (type node) :directory))

(defn filehandle? [node] (= (type node) :filehandle))



(defn split-path [path] (rest (partition path #"/")))

(defn- path-match-to-keys [path]
  (match path
    ["/" dir "/" file] (list :files dir :files file)
    ["/" dir "/" & rest] (concat (list :files dir :files) (path-match-to-keys rest))
    [dir "/" & rest] (concat (list dir :files) (path-match-to-keys rest))
    ["/" file] (list :files file)))

(defn lookup-keys [path]
  (path-match-to-keys (split-path path)))

(defn lookup [path]
  (if (= path "/") @root
    (get-in @root (lookup-keys path))))

(defn under-root? [path]
  (= (lookup (-> path (File.) (.getParent))) @root))

(defn- update [path key value]
  (dosync (ref-set root
    (if (= path "/") (assoc-in @root (list key) value)
      (assoc-in @root (concat (lookup-keys path) (list key)) value)))))

(defn update-atime [path value]
  (update path :lastmod (/ value 1000)))

(defn update-mode [path value]
  (update path :mode value))

(defn add-file [path file]
  (dosync (ref-set root (assoc-in @root (lookup-keys path) file))))

(defn remove-file [path]
  (dosync (ref-set root (assoc-in @root (lookup-keys path) nil))))

(defn create-handle [metadata]
  (let [type-data {:type :filehandle}]
    (proxy [clojure.lang.IObj] []
      (withMeta [meta] (merge type-data metadata))
      (meta [] (merge type-data metadata))
      (toString [] (str "handle for " (:node metadata)))
      (finalize [] (println "finalizing")))))

