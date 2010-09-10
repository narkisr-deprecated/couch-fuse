(ns com.narkisr.couchfs.couch-file
  (:require [com.narkisr.couchfs.couch-access :as couch] 
            [com.narkisr.couchfs.file-update :as file-update]
            [com.narkisr.fs-logic :as fs-logic]
            [com.narkisr.protocols :as proto]
            [com.narkisr.couchfs.initialization :as init])
  (:use 
     (com.narkisr common-fs file-info)
     (couchdb (client :only [ResourceConflict]))
     (clojure.contrib (error-kit :only [handle with-handler]) (json :only [read-json]))))

(defn update-file [path file contents-str]
  (let [{:keys [couch-id attachment]} (fs-logic/xattr-map file)]
    (with-handler
      (if attachment 
        (file-update/update-attachment path couch-id (:name file) contents-str)
        (file-update/update-rev-and-time path (couch/update-document couch-id (read-json contents-str))))
      (handle ResourceConflict [msg] 
              (file-update/use-lastest-rev path couch-id (read-json contents-str))))))

(defn create-file [path mode]
  "Adds an empty attachment to the given document path, update file will fill the missing data"
   (proto/create (init/attachment (parent-name path) (fname path) {:content_type "" :length 0}) path))

(defn delete-file [path]
  "Delete only attachments"
  (proto/delete (fs-logic/lookup path)))

(defn fetch-content
  ([file] (-> file :content (apply [])))
  ([file f] (-> file :content (apply []) f)))

(defn fetch-size [file] 
  (-> file :size (apply [])))

(defn rename-file [from to]
  "This is expensive, couch does not have a rename built in https://issues.apache.org/jira/browse/COUCHDB-715"
  (let [content (String. (fetch-content (fs-logic/lookup from)))]
    (delete-file from)
    (create-file to 0644)
    (update-file to (fs-logic/lookup to) content)))
