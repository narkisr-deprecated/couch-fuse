(ns com.narkisr.couchfs.couch-fs
  (:require [com.narkisr.couchfs.write-cache :as cache]
            [com.narkisr.couchfs.couch-file :as couch-file]
            [com.narkisr.file-info :as info]
            [com.narkisr.fs-logic :as fs-logic]
            [com.narkisr.couchfs.initialization :as init]
            [com.narkisr.protocols :as proto])
  (:use (com.narkisr common-fs))
  (:import fuse.FuseFtypeConstants fuse.Errno 
           com.narkisr.protocols.MetaFolder
           org.apache.commons.logging.LogFactory))

(def BLOCK_SIZE 512)

(gen-class :name com.narkisr.couch-fuse :implements [fuse.Filesystem3] :prefix "fs-")

(def-fs-fn getdir [path filler] 
  {:pre [[(fs-logic/directory? (fs-logic/lookup path)) Errno/ENOTDIR]]}
  (let [node (fs-logic/lookup path)]
    (doseq [child (-> node :files vals) :let [ftype (proto/fuse-const child)] :when ftype]
      (. filler add (:name child) (. child hashCode) (bit-or ftype (:mode child))))))

(defn- apply-attr [setter node fuse-type length]
  (. setter set (. node hashCode)
    (bit-or fuse-type (:mode node)) 1 0 0 0 length (/ (+ length (- BLOCK_SIZE 1)) BLOCK_SIZE) (:lastmod node ) (:lastmod node ) (:lastmod node)))

(def-fs-fn getattr [path setter] 
  {:pre [[(satisfies? proto/FsNode (fs-logic/lookup path)) Errno/ENOENT]]}
  (let [node (fs-logic/lookup path) ]
      (apply-attr setter node (proto/fuse-const node) (proto/size node))))

(def-fs-fn open [path flags openSetter]
  (let [node (fs-logic/lookup path)]
    (. openSetter setFh (fs-logic/create-handle {:node node :content (couch-file/fetch-content node)}))))

(def-fs-fn read [path fh buf offset] 
  {:pre [[(fs-logic/filehandle? fh) Errno/EBADF]]}
  (let [file (-> fh meta :node) content (-> fh meta :content)]
    (. buf put content offset (min (. buf remaining) (- (alength content) offset)))))

(def-fs-fn flush [path fh] 
  {:pre [[(fs-logic/filehandle? fh) Errno/EBADF]]}
  (if (contains? @cache/write-cache path)
    (try
      (couch-file/update-file path (-> fh meta :node) (String. (@cache/write-cache path)))
     (catch Exception e (log-warn this (str (. e getMessage) (class e))))
     (finally (cache/clear-cache path)) ; no point in keeping bad cache values
      )))

(def-fs-fn release [path fh flags] 
  {:pre [[(fs-logic/filehandle? fh) Errno/EBADF]]}
  (System/runFinalization))

(def-fs-fn truncate [path size])

(def-fs-fn write [path fh is-writepage buf offset] 
  {:pre [[(fs-logic/filehandle? fh) Errno/EROFS]]}
  (let [total-written (min (. buf remaining) 256) b (byte-array total-written)]
    (. buf get b 0 total-written)
    (cache/update-cache path b)
    total-written))

(def-fs-fn mknod [path mode rdev] 
  (proto/create 
    (init/attachment (info/parent-name path) (info/fname path) {:content_type "" :length 0}) path))

(def-fs-fn mkdir [path mode] 
  {:pre [[(fs-logic/under-root? path) Errno/EPERM]]}
  (let [couch-id (info/fname path) parent (info/parent-path path)]
   (proto/create (init/content-folder couch-id) (info/combine parent couch-id))
   (proto/create (init/meta-folder couch-id (info/hide couch-id)) (info/combine parent (info/hide couch-id)))))

(def-fs-fn utime [path atime mtime]
  (fs-logic/update-atime path mtime))

(def-fs-fn chmod [path mode]
  (fs-logic/update-mode path mode))

(def-fs-fn fsync [path fh isDatasync]
  (log-warn "" "fsync not impl"))

(def-fs-fn unlink [path] 
  (if (-> (fs-logic/lookup path) fs-logic/xattr-map :attachment)
    (proto/delete (fs-logic/lookup path) path)))

(def-fs-fn chown [path uid gid]
  (log-warn "" "chwon not impl"))

(def-fs-fn rename [from to]
  (couch-file/rename-file from to))

(defn meta-and-content-exists? [path]
  (and (instance? MetaFolder (fs-logic/lookup path)) (fs-logic/lookup (info/un-hide path))))

(def-fs-fn rmdir [path] 
  {:pre [[(fs-logic/under-root? path)] [(-> path meta-and-content-exists? not)]] :default Errno/EPERM}
    (proto/delete (fs-logic/lookup path) path))

; file systems stats
(def-fs-fn statfs [statfs-setter]
  (. statfs-setter set BLOCK_SIZE 1000 200 180 (-> @fs-logic/root :files (. size)) 0 proto/NAME_LENGTH))

