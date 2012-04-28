(ns com.narkisr.common-test
  (:require [com.narkisr.couchfs.couch-access :as couch])
  (:import java.io.File)
  (:use
    [clojure.test :only [is]]
    (com.narkisr.couchfs (mounter :only [mount-with-group]))
    (clojure.java (shell :only [sh]))
    (com.narkisr fs-logic)
    [clojure.tools.trace :only [trace-ns trace-vars]]
    ))


(def meta-file)
(def uuid)

(def rename-path)
(def file-path)

(defn assert-sh [& v]
  {:pre [(some #{:out :err} v)]}
  "assrts a shell cmd output against expected output key"
  (let [[exec [k] [asrt]] (partition-by keyword? v)]
    (is (= (k (apply sh exec)) asrt))))

;(trace-ns 'com.narkisr.couchfs.couch-access)
;(trace-ns 'com.narkisr.couchfs.couch-fs)
;(trace-vars com.narkisr.couchfs.couch-fs/fs-mkdir)

(defn mount-and-sleep [f]
  (def uuid (str (java.util.UUID/randomUUID)))
  (def meta-file (-> (str "fake/." uuid "/" uuid ".json")))
  (def rename-path (str "fake/" uuid "/" uuid "renamed.html"))
  (def file-path (str "fake/" uuid "/" uuid ".html"))
  (couch/create-non-existing-db "playground")
  (sh "mkdir" "fake")
  (mount-with-group "http://127.0.0.1:5983/" "playground" "fake" "fuse-threads")
  (java.lang.Thread/sleep 1000)
  (sh "mkdir" (str "fake/" uuid))
  (f)
  (sh "rm" "-r" (str "fake/" uuid))
  (sh "rm" "-r" (str "fake/." uuid))
  (sh "fusermount" "-u" "fake") 
  (couch/delete-db "playground"))


