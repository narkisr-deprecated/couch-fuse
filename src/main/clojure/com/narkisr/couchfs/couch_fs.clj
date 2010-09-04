(ns com.narkisr.couchfs.couch-fs
  (:require [com.narkisr.couchfs.write-cache :as cache]
            [com.narkisr.couchfs.couch-file :as couch-file]
            [com.narkisr.protocols :as proto])
  (:use (com.narkisr fs-logic common-fs file-info))
  (:import fuse.FuseFtypeConstants fuse.Errno 
           com.narkisr.protocols.MetaFolder
           org.apache.commons.logging.LogFactory))

(def BLOCK_SIZE 512)

(gen-class :name com.narkisr.couch-fuse :implements [fuse.Filesystem3] :prefix "fs-")

(def-fs-fn getdir [path filler] (directory? (lookup path)) Errno/ENOTDIR
  (let [node (lookup path)]
    (doseq [child (-> node :files vals) :let [ftype (proto/fuse-const child)] :when ftype]
      (. filler add (:name child) (. child hashCode) (bit-or ftype (:mode child))))))

(defn- apply-attr [setter node fuse-type length]
  (. setter set (. node hashCode)
    (bit-or fuse-type (:mode node)) 1 0 0 0 length (/ (+ length (- BLOCK_SIZE 1)) BLOCK_SIZE) (:lastmod node ) (:lastmod node ) (:lastmod node)))

(def-fs-fn getattr [path setter] (satisfies? proto/FsNode (lookup path)) Errno/ENOENT
  (let [node (lookup path) ]
      (apply-attr setter node (proto/fuse-const node) (proto/size node))))

(def-fs-fn open [path flags openSetter]
  (let [node (lookup path)]
    (. openSetter setFh (create-handle {:node node :content (couch-file/fetch-content node)}))))

(def-fs-fn read [path fh buf offset] (filehandle? fh) Errno/EBADF
  (let [file (-> fh meta :node) content (-> fh meta :content)]
    (. buf put content offset (min (. buf remaining) (- (alength content) offset)))))

(def-fs-fn flush [path fh] (filehandle? fh) Errno/EBADF
  (if (contains? @cache/write-cache path)
    (try
      (couch-file/update-file path (-> fh meta :node) (String. (@cache/write-cache path)))
     (catch Exception e (log-warn this (str (. e getMessage) (class e))))
     (finally (cache/clear-cache path)) ; no point in keeping bad cache values
      )))

(def-fs-fn release [path fh flags] (filehandle? fh) Errno/EBADF (System/runFinalization))

(def-fs-fn truncate [path size])

(def-fs-fn write [path fh is-writepage buf offset] (filehandle? fh) Errno/EROFS
  (let [total-written (min (. buf remaining) 256) b (byte-array total-written)]
    (. buf get b 0 total-written)
    (cache/update-cache path b)
    total-written))

(def-fs-fn mknod [path mode rdev] 
  (couch-file/create-file path mode))

(def-fs-fn mkdir [path mode] (under-root? path) Errno/EPERM
  (couch-file/create-folder path mode))

(def-fs-fn utime [path atime mtime]
  (update-atime path mtime))

(def-fs-fn chmod [path mode]
  (update-mode path mode))

(def-fs-fn fsync [path fh isDatasync]
  (log-warn "" "fsync not impl"))

(def-fs-fn unlink [path] 
  (if (-> (lookup path) xattr-map :attachment)
    (couch-file/delete-file path)))

(def-fs-fn chown [path uid gid]
  (log-warn "" "chwon not impl"))

(def-fs-fn rename [from to]
  (couch-file/rename-file from to))

(def-fs-fn rmdir [path] (and (under-root? path) (not (and (instance? MetaFolder (lookup path)) (lookup (un-hide path))))) Errno/EPERM
    (if (instance? MetaFolder (lookup path))
      (couch-file/delete-meta-folder path)
      (couch-file/delete-folder path)))

; file systems stats
(def-fs-fn statfs [statfs-setter]
  (. statfs-setter set BLOCK_SIZE 1000 200 180 (-> @root :files (. size)) 0 proto/NAME_LENGTH))

