(ns com.narkisr.couchfs.couch-access
  (:refer-clojure :exclude [contains?])
  (:require [clj-http.client :as client])
  (:use
    [slingshot.slingshot :only [try+]]
    (com.narkisr fs-logic)
    [clojure.java.io :only [copy]]
    (couchdb 
      (client :only [database-create document-list document-get document-update document-delete document-create view-get attachment-get attachment-list attachment-create attachment-delete database-delete]))
    )
  (:import 
    (java.io ByteArrayOutputStream InputStream)
    (java.net URL)))

(def ^:dynamic *host* "http://localhost:5984/")
(def ^:dynamic *db* "playground")

(defn couch [fn & params]
  "Applies the binded couch configuration on the given fn, this cannot be a macro since the values are binded post the expansion stage"
  (apply fn *host* *db* params))

(defn- not-design-id [id]
  (not (.contains id "design")))

(defn all-ids []
  (lazy-seq (filter not-design-id (couch document-list))))

(defn attachments [name]
  (try (couch attachment-list name)
    (catch java.io.FileNotFoundException e {})))

(defn add-attachment [id name contents mimetype]
  (couch attachment-create id name contents mimetype))

(defn delete-attachment [id name]
  (couch attachment-delete id name ))

(defn couch-size [path]
  "Fetches file size in bytes using http HEAD, note that size is + 1 more than the actual content size."
  (fn [] 
    (let [u (str *host* *db* "/" path) h (client/head u)] 
      (-> h (get-in [:headers "content-length"]) Integer/parseInt (- 1)))))


(defn update-document [id contents]
  (couch document-update id contents))

(defn create-document [id]
  (couch document-create id {}))

(defn delete-document [id]
  (couch document-delete id))

(defn get-document [id]
  (couch document-get id))

(defn couch-content [name]
  (fn [] 
    (-> (str *host* *db* "/" name) client/get :body (. getBytes))))

(defn to-byte-array [#^InputStream x]
  (let [buffer (ByteArrayOutputStream.)]
    (copy x buffer)
    (.toByteArray buffer)))

(defn couch-attachment-content [doc attachment]
  (fn [] (-> (URL. (couch str "/" doc "/" attachment)) (. openConnection) (. getInputStream) to-byte-array)))

(defn db-exists? [host db]
  (try+ (client/get (str host db))
    (catch identity e nil)))

(defn create-non-existing-db [name]
  (when-not (db-exists? *host* name)
    (database-create *host* name)))

(defn delete-db [name] (database-delete *host* name))

