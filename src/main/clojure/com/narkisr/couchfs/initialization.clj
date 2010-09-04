(ns com.narkisr.couchfs.initialization
  (:import com.narkisr.protocols.Directory 
           com.narkisr.protocols.MetaFolder
           com.narkisr.protocols.File)
  (:require [com.narkisr.couchfs.couch-access :as couch])
  (:use (com.narkisr (file-info :only [fname parent-path combine parent-name file-path un-hide]) (fs-logic ))))

(defn- inner-file [file-name desc mime content-fn size-fn couch-id ]
  (with-type :file (File. file-name 0644 [:description desc :mimetype mime :couch-id couch-id] (/ (System/currentTimeMillis) 1000) content-fn size-fn)))

(defn- json-file [couch-id]
  (let [file-name (str couch-id ".json")]
    {file-name (inner-file file-name "A Couchdb json document" "application/json" (couch/couch-content couch-id) (couch/couch-size couch-id) couch-id )}))

(defn attachment [couch-id file-name details]
  (with-xattr [:attachment true]
              (inner-file file-name  "A Couchdb document attachment" (details :content_type) (couch/couch-attachment-content couch-id file-name) #(details :length) couch-id) ))

(defn- attachments [couch-id]
  (reduce (fn [res [file-name details]] (assoc res file-name (attachment couch-id file-name details))) {} (couch/attachments couch-id)))

(defn document-folder [couch-id]
  (let [hidden (str "." couch-id)]
    (merge {hidden (with-xattr [:meta-folder true]
                     (with-type :directory (MetaFolder. hidden 0444 [:description "Couch meta folder"] (/ (System/currentTimeMillis) 1000) (json-file couch-id))))}
           {couch-id (with-type :directory (Directory. couch-id 0755 [:description "Couch attachments folder"] (/ (System/currentTimeMillis) 1000) (attachments couch-id)))})))

(defn couch-files []
  (reduce merge (map #(document-folder %) (couch/all-ids))))

(defn init-fs-root []
  (dosync (ref-set root (create-node directory "" 0755 [:description "Couchdb directory"] (couch-files)))))

