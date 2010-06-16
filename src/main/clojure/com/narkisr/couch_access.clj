(ns com.narkisr.couch-access
  (:import (java.net URL))
  (:refer-clojure :exclude [contains?])
  (:require [clojure.http.resourcefully :as resourcefully])
  (:use
    (com.narkisr fs-logic)
    (couchdb (client :only [document-list document-get document-update document-delete document-create view-get attachment-get attachment-list]))
    (clojure.contrib (str-utils2 :only [contains?]) error-kit (def :only [defn-memo]) duck-streams)
    (clojure.contrib.json read write)))

(def *host* "http://127.0.0.1:5984/")
(def *db* "blog-import")

(defn couch [fn & params]
  "Applies the binded couch configuration on the given fn, this cannot be a macro since the values are binded post the expansion stage"
  (apply fn *host* *db* params))

(defn- not-design-id [id]
  (not (contains? id "design")))

(defn all-ids []
  (lazy-seq (filter not-design-id (couch document-list))))

(defn attachments [name]
  (couch attachment-list name))

(defn couch-size [path]
  "Fetches file size in bytes using http HEAD, note that size is + 1 more than the actual content size."
  (fn [] (-> (str *host* *db* "/" path) resourcefully/head (get-in [:headers :content-length]) first Integer/parseInt (- 1))))

(defn update-document [id contents]
  (couch document-update id (read-json contents)))

(defn create-document [id]
  (couch document-create id {}))

(defn delete-document [id]
  (couch document-delete id))

(defn couch-content [name]
  (fn [] (-> (str *host* *db* "/" name) resourcefully/get :body-seq first (. getBytes))))

(defn couch-attachment-content [doc attachment]
  (fn [] (-> (URL. (couch str "/" doc "/" attachment)) (. openConnection) (. getInputStream) to-byte-array)))

(defn db-exists? [host db]
  (try (resourcefully/get (str host db))
    (catch java.io.IOException e nil)))

