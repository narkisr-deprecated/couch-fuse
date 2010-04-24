(ns com.narkisr.mounter
  (:gen-class)
  (:import fuse.FuseMount )
  (:use com.narkisr.fake-fs))


(defn -main []
  (FuseMount/mount
    (into-array ["/home/ronen/CodeProjects/couch-fuse/fake" "-f"])
    (new com.narkisr.fake) log))
