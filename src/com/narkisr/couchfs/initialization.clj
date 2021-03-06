(ns com.narkisr.couchfs.initialization
  (:import com.narkisr.protocols.Directory 
           com.narkisr.protocols.Root
           com.narkisr.protocols.MetaFolder
           com.narkisr.protocols.File
           couchdb.errors.DocumentNotFound)
  (:require 
    [com.narkisr.couchfs.couch-access :as couch]
     [couchdb.errors] 
    )
  (:use 
    [slingshot.slingshot :only [try+]]
    (com.narkisr 
      (file-info :only [fname parent-path combine parent-name file-path un-hide hide]) 
      (fs-logic))))

(defn- inner-file [file-name desc mime content-fn size-fn couch-id ]
  (File. file-name 0644 [:description desc :mimetype mime :couch-id couch-id] (/ (System/currentTimeMillis) 1000) content-fn size-fn))

(defn- json-file [couch-id]
  (let [file-name (str couch-id ".json")]
    {file-name (inner-file file-name "A Couchdb json document" "application/json" (couch/couch-content couch-id) (couch/couch-size couch-id) couch-id )}))

(defn attachment [couch-id file-name details]
  (with-xattr [:attachment true]
    (inner-file file-name  "A Couchdb document attachment" (details :content_type) (couch/couch-attachment-content couch-id file-name) #(details :length) couch-id) ))

(defn safe-attaments [couch-id]
  (try+ (couch/attachments couch-id)
        (catch DocumentNotFound _ [])))

(defn- attachments [couch-id]
  (reduce 
    (fn [res [file-name details]] 
      (assoc res file-name (attachment couch-id file-name details))) {} (safe-attaments couch-id)))

(defn meta-folder [couch-id hidden]
  (MetaFolder. hidden 0444 [:description "Couch meta folder"] (/ (System/currentTimeMillis) 1000) (json-file couch-id)))

(defn content-folder [couch-id]
  (Directory. couch-id 0755 [:description "Couch attachments folder"] (/ (System/currentTimeMillis) 1000) (attachments couch-id)))

(defn document-folders [couch-id]
  (let [hidden (hide couch-id)] 
    {couch-id (content-folder couch-id) hidden (meta-folder couch-id hidden)}))

(defn couch-files []
  (reduce merge (map #(document-folders %) (couch/all-ids))))

(defn init-fs-root []
  (dosync (ref-set root (Root. ""  0755 [:description "Root directory"] (/ (System/currentTimeMillis) 1000) (or (couch-files) {})))))

