(ns com.narkisr.couchfs.couch-file
  (:require [com.narkisr.couchfs.couch-access :as couch] 
            [com.narkisr.couchfs.file-update :as file-update]
            [com.narkisr.fs-logic :as fs-logic]
            [com.narkisr.protocols :as proto]
            [com.narkisr.couchfs.initialization :as init]
            [couchdb.errors]
            )
  (:use 
    [slingshot.slingshot :only [try+]]
    [cheshire.core :only [parse-string ]]
    (com.narkisr common-fs file-info)
    )
  (:import (couchdb.errors ResourceConflict) ) 
  )

(defn update-file [path file contents-str]
  (let [{:keys [couch-id attachment]} (fs-logic/xattr-map file)]
    (try+
      (if attachment 
        (file-update/update-attachment path couch-id (:name file) contents-str)
        (file-update/update-rev-and-time path (couch/update-document couch-id (parse-string contents-str))))
      (catch ResourceConflict e
        (file-update/use-lastest-rev path couch-id (parse-string contents-str))))))

(defn fetch-content
  ([file] (-> file :content (apply [])))
  ([file f] (-> file :content (apply []) f)))

(defn rename-file [from to]
  "This is expensive, couch does not have a rename built in https://issues.apache.org/jira/browse/COUCHDB-715,
  Adds an empty attachment to the given document path, update file will fill the missing data"
  (let [content (String. (fetch-content (fs-logic/lookup from)))]
    (proto/delete (fs-logic/lookup from) from)
    (proto/create (init/attachment (parent-name to) (fname to) {:content_type "" :length 0}) to) 
    (update-file to (fs-logic/lookup to) content)))
