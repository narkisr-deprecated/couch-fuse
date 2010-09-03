(ns com.narkisr.couchfs.initialization
  (:require [com.narkisr.couchfs.couch-access :as couch])
  (:use 
     (com.narkisr (fs-logic :only [create-node file with-xattr directory root]))
     ))

(defn- inner-file [file-name desc mime content-fn size-fn couch-id ]
  (create-node file file-name 0644 [:description desc :mimetype mime :couch-id couch-id] content-fn size-fn))

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
                               (create-node directory hidden 0444 [:description "Couch meta folder"] (json-file couch-id)))}
           {couch-id (create-node directory couch-id 0755 [:description "Couch attachments folder"]  (attachments couch-id))})))

(defn couch-files []
  (reduce merge (map #(document-folder %) (couch/all-ids))))

(defn init-fs-root []
  (dosync (ref-set root (create-node directory "" 0755 [:description "Couchdb directory"] (couch-files)))))
