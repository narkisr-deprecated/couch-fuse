(ns com.narkisr.protocols
  (:use (clojure.contrib (def :only [defmacro-])) 
        (com.narkisr common-fs file-info))
  (:require [com.narkisr.couchfs.couch-access :as couch] 
            [com.narkisr.couchfs.file-update :as file-update]
            [com.narkisr.fs-logic :as fs-logic]
            [com.narkisr.couchfs.initialization :as init]))

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
  (delete [this path])
  (create [this path mode])
  (read [this]))

(defprotocol FsMeta
  (xattr [this])
  (size [this]))

(extend-type Directory 
  Crud
   (delete [this path]
     (fs-logic/remove-file path))
   (create [this path mode]
     (let [couch-id (fname path) parent (parent-path path)]
      (couch/create-document couch-id)
      (doseq [[k v] (init/document-folder couch-id)]
       (fs-logic/add-file (combine parent k) v))))

   (read [this]))
