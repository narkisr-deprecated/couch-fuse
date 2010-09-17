(ns com.narkisr.common-test
  (:require [com.narkisr.couchfs.couch-access :as couch])
  (:import java.io.File)
  (:use
    (com.narkisr.couchfs (mounter :only [mount-with-group]))
    (com.narkisr fs-logic )
    (clojure.contrib shell-out )))

(def meta-file)
(def uuid)

(def rename-path)
(def file-path)

(defn mount-and-sleep [f]
  (def uuid (java.util.UUID/randomUUID))
  (def meta-file (-> (str "fake/." uuid "/" uuid ".json")))
  (def rename-path (str "fake/" uuid "/" uuid "renamed.html"))
  (def file-path (str "fake/" uuid "/" uuid ".html"))
  (couch/delete-db "playground")
  (couch/create-non-existing-db "playground")
  (mount-with-group "http://127.0.0.1:5984/" "playground" "fake" "fuse-threads")
  (java.lang.Thread/sleep 1000)
  (sh "mkdir" (str "fake/" uuid))
  (f)
  (sh "rm" "-r" (str "fake/" uuid))
  (sh "rm" "-r" (str "fake/." uuid))
  (sh "fusermount" "-u" "fake"))


