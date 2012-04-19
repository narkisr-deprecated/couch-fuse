(ns com.narkisr.protocols 
  (:import fuse.FuseFtypeConstants)
  (:use (clojure.contrib (def :only [defmacro-])) 
        (com.narkisr common-fs file-info))
  (:require [com.narkisr.couchfs.couch-access :as couch] 
            [com.narkisr.couchfs.file-update :as file-update]
            [com.narkisr.fs-logic :as fs-logic]))

(def NAME_LENGTH 1024)

(defn with-type [type x]
  (with-meta x {:type type}))

(defn into-syms [keys]
  (vec (map #(. % sym) (into [:name :mode :xattrs :lastmod] keys))))

(defmacro- fstype [name & keys]
  `(defrecord ~name ~(into-syms keys)))

(fstype Root :files)
(fstype Directory :files)
(fstype MetaFolder :files)
(fstype File :content :size)

(defprotocol FsNode
  (delete [this path])
  (create [this path]))

(defprotocol FsMeta (size [this]))

(defprotocol Fusable (fuse-const [this]))

(extend-type Root 
  FsNode
   (delete [this path])
   (create [this path])
  FsMeta
   (size [this] (* (. (or (:files this) '())  size) NAME_LENGTH))
  Fusable 
   (fuse-const [this]  FuseFtypeConstants/TYPE_DIR))

(extend-type Directory 
  FsNode
   (delete [this path]
     (fs-logic/remove-file path))
   (create [this path]
    (let [couch-id (fname path) parent (parent-path path)]
      (couch/create-document couch-id)
      (fs-logic/add-file path this)))
  FsMeta
   (size [this] (* (. (:files this)  size) NAME_LENGTH))
  Fusable 
   (fuse-const [this]  FuseFtypeConstants/TYPE_DIR))

(extend-type File
  FsNode
   (delete [this path]
     (let [couch-id (parent-name path) attach-id (fname path)]
      (couch/delete-attachment couch-id attach-id)
      (fs-logic/remove-file path)))
   (create [this path] 
     (let [couch-id (parent-name path) attach-id (fname path)]
      (couch/add-attachment couch-id attach-id "" "text/plain")
      (fs-logic/add-file (file-path path) this)))
  FsMeta
   (size [this] (-> this :size (apply [])))
  Fusable 
   (fuse-const [this]  FuseFtypeConstants/TYPE_FILE))

(extend-type MetaFolder
  FsNode
   (delete [this path]
     (let [doc-id (-> path un-hide fname)]
      (couch/delete-document doc-id)
      (fs-logic/remove-file path)))
   (create [this path] 
     (fs-logic/add-file path this))
  FsMeta
   (size [this] (* (. (:files this)  size) NAME_LENGTH))
  Fusable 
   (fuse-const [this]  FuseFtypeConstants/TYPE_DIR))
