(ns com.narkisr.couch-fs
  (:use com.narkisr.mocking com.narkisr.fs-logic com.narkisr.common-fs com.narkisr.couch-file)
  (:import fuse.FuseFtypeConstants fuse.Errno org.apache.commons.logging.LogFactory))

(defn bind-root []
  (alter-var-root #'root (fn [_] (create-node directory "" 0755 [:description "Couchdb directory"] (couch-files)))))

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
  (println "flushing")
  )

(def-fs-fn release [path fh flags] (filehandle? fh) Errno/EBADF (System/runFinalization))

(def-fs-fn truncate [path size]
  (println "resizing")
  )

(def-fs-fn write [path fh is-writepage buf offset] (filehandle? fh) Errno/EROFS
  (let [b (byte-array 256)]
    (do
      (println (lookup-keys path))
      (println (. buf get b offset (min (. buf remaining) (- (alength b) offset)))))))

(def-fs-fn mknod [path mode rdev]
  (add-file path mode))

(def-fs-fn utime [path atime mtime]
  (update-atime path mtime))

(def-fs-fn chmod [path mode]
  (update-mode path mode))

(def-fs-fn fsync [path fh isDatasync]
  (println "fsync"))

(def-fs-fn unlink [path]
  (remove-file path))

; file systems stats
(def-fs-fn statfs [statfs-setter]
  (. statfs-setter set BLOCK_SIZE 1000 200 180 (-> root :files (. size)) 0 NAME_LENGTH))
