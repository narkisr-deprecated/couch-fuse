(ns com.narkisr.mounter
  (:gen-class)
  (:import fuse.FuseMount org.apache.commons.logging.LogFactory)
  (:use com.narkisr.fake-fs))


(defn -main []
  (FuseMount/mount
    (into-array ["/home/ronen/CodeProjects/couch-fuse/fake" "-f"])
    (new com.narkisr.fake) (LogFactory/getLog (class com.narkisr.mounter))))
