(ns com.narkisr.mounter
  (:gen-class)
  (:import fuse.FuseMount org.apache.commons.logging.LogFactory)
  (:use com.narkisr.couch-fs))


(defn -main [path]
  (FuseMount/mount
    (into-array  [path "-f"])
    (com.narkisr.couch-fuse.) (LogFactory/getLog (class com.narkisr.mounter))))
