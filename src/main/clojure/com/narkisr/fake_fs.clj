(ns com.narkisr.fake-fs
  (:use com.narkisr.mocking com.narkisr.fs-logic com.narkisr.common-fs)
  (:import fuse.FuseFtypeConstants fuse.Errno ))

; see @ http://omake.metaprl.org/prerelease/omake-dll-fuse.html

(def NAME_LENGTH 1024)
(def BLOCK_SIZE 512)

(gen-class :name com.narkisr.fake :implements [fuse.Filesystem3] :prefix "fs-")

(def-fs-fn fs-getdir [path filler] (directory? (lookup path)) Errno/ENOTDIR
  (let [node (lookup path) type-to-const {:directory FuseFtypeConstants/TYPE_DIR :file FuseFtypeConstants/TYPE_FILE :link FuseFtypeConstants/TYPE_SYMLINK}]
    (doseq [child (-> node :files vals) :let [ftype (type-to-const (type child))] :when ftype]
      (. filler add (child :name) (. child hashCode) (bit-or ftype (child :mode))))))


(defn- apply-attr [setter node type length]
  (. setter set (. node hashCode)
    (bit-or type (node :mode)) 1 0 0 0 length (/ (+ length (- BLOCK_SIZE 1)) BLOCK_SIZE) (node :lastmod) (node :lastmod) (node :lastmod)))

(def-fs-fn fs-getattr [path setter] (some #{(type (lookup path))} [:directory :file :link]) Errno/ENOENT
  (let [node (lookup path)]
    (condp = (type node)
      :directory (apply-attr setter node FuseFtypeConstants/TYPE_DIR (* (-> node :files (. size)) NAME_LENGTH)) ; TODO change size to clojure idioum
      :file (apply-attr setter node FuseFtypeConstants/TYPE_FILE (-> node :content alength))
      :link (apply-attr setter node FuseFtypeConstants/TYPE_SYMLINK (-> node :link (. size)))
      )))

(def-fs-fn fs-open [path flags openSetter]
  (let [node (lookup path)]
    (. openSetter setFh (create-handle {:node node}))))

(def-fs-fn fs-read [path fh buf offset] (filehandle? fh) Errno/EBADF
  (let [file (-> fh meta :node)]
    (. buf put (file :content) offset (min (. buf remaining) (- (-> file :content alength) offset)))))


(def-fs-fn fs-flush [path fh] (filehandle? fh) Errno/EBADF)

(def-fs-fn fs-release [path fh flags] (filehandle? fh) Errno/EBADF (System/runFinalization))

(def-fs-fn fs-truncate [path size])

(def-fs-fn fs-write [path fh is-writepage buf offset] false Errno/EROFS)

; file systems stats
(def-fs-fn fs-statfs [statfs-setter]
  (. statfs-setter set BLOCK_SIZE 1000 200 180 (-> root :files (. size)) 0 NAME_LENGTH))

