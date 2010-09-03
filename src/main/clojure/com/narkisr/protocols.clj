(ns com.narkisr.protocols
  (:use (clojure.contrib (def :only [defmacro-]))))

(defn with-type [type x]
  (with-meta x {:type type}))

(defn into-syms [keys]
  (vec (map #(. % sym) (into [:name :mode :xattrs :lastmod] keys))))

(defmacro fstype [name & keys]
  `(defrecord ~name ~(into-syms keys)))

(fstype Directory :files)
(fstype Meta :file)
(fstype File :content :size)

(defprotocol Crud
  (delete [this])
  (create [this])
  (read [this]))

(defprotocol FsMeta
  (xattr [this])
  (size [this]))

(extend-type Directory 
  Crud
   (delete [this])
   (create [this])
   (read [this]))
