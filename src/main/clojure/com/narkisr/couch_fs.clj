(ns com.narkisr.couch-fs
  (:use (com.narkisr fs-logic common-fs couch-file write-cache))
  (:import fuse.FuseFtypeConstants fuse.Errno org.apache.commons.logging.LogFactory))

(defn bind-root []
  (dosync (ref-set root (create-node directory "" 0755 [:description "Couchdb directory"] (couch-files)))))

(def NAME_LENGTH 1024)
(def BLOCK_SIZE 512)

(gen-class :name com.narkisr.couch-fuse :implements [fuse.Filesystem3] :prefix "fs-")

(def type-to-const {:directory FuseFtypeConstants/TYPE_DIR :file FuseFtypeConstants/TYPE_FILE :link FuseFtypeConstants/TYPE_SYMLINK})

(def-fs-fn getdir [path filler] (directory? (lookup path)) Errno/ENOTDIR
  (let [node (lookup path)]
    (doseq [child (-> node :files vals) :let [ftype (type-to-const (type child))] :when ftype]
      (. filler add (child :name) (. child hashCode) (bit-or ftype (child :mode))))))


(defn- apply-attr [setter node type length]
  (. setter set (. node hashCode)
    (bit-or type (node :mode)) 1 0 0 0 length (/ (+ length (- BLOCK_SIZE 1)) BLOCK_SIZE) (node :lastmod) (node :lastmod) (node :lastmod)))

(def-fs-fn getattr [path setter] (some #{(type (lookup path))} [:directory :file :link]) Errno/ENOENT
  (let [node (lookup path)]
    (condp = (type node)
      :directory (apply-attr setter node FuseFtypeConstants/TYPE_DIR (* (-> node :files (. size)) NAME_LENGTH)) ; TODO change size to clojure idiom
      :file (apply-attr setter node FuseFtypeConstants/TYPE_FILE (fetch-size node))
      :link (apply-attr setter node FuseFtypeConstants/TYPE_SYMLINK (-> node :link (. size)))
      )))

(def-fs-fn open [path flags openSetter]
  (let [node (lookup path)]
    (. openSetter setFh (create-handle {:node node :content (fetch-content node)}))))

(def-fs-fn read [path fh buf offset] (filehandle? fh) Errno/EBADF
  (let [file (-> fh meta :node) content (-> fh meta :content)]
    (. buf put content offset (min (. buf remaining) (- (alength content) offset)))))

(def-fs-fn flush [path fh] (filehandle? fh) Errno/EBADF
  (if (contains? @write-cache path)
    (try
      (update-file path (-> fh meta :node) (String. (@write-cache path)))
      (catch Exception e (log-warn this (str (. e getMessage) (class e))))
      (finally (clear-cache path)) ; no point in keeping bad cache values
      )))

(def-fs-fn release [path fh flags] (filehandle? fh) Errno/EBADF (System/runFinalization))

(def-fs-fn truncate [path size])

(def-fs-fn write [path fh is-writepage buf offset] (filehandle? fh) Errno/EROFS
  (let [total-written (min (. buf remaining) (- 256 offset)) b (byte-array total-written)]
    (. buf get b offset total-written)
    (update-cache path b)
    total-written))

(def-fs-fn mknod [path mode rdev] 
  (create-file path mode))

(def-fs-fn mkdir [path mode] (under-root? path) Errno/EPERM
  (create-folder path mode))

(def-fs-fn utime [path atime mtime]
  (update-atime path mtime))

(def-fs-fn chmod [path mode]
  (update-mode path mode))

(def-fs-fn fsync [path fh isDatasync])

(def-fs-fn unlink [path])

(def-fs-fn chown [path uid gid])

(def-fs-fn rename [from to])

(def-fs-fn rmdir [path] (under-root? path) Errno/EPERM
  (delete-folder path))

; file systems stats
(def-fs-fn statfs [statfs-setter]
  (. statfs-setter set BLOCK_SIZE 1000 200 180 (-> @root :files (. size)) 0 NAME_LENGTH))
