(ns com.narkisr.fake-fs
  (:use com.narkisr.mocking com.narkisr.fs-logic)
  (:import fuse.FuseFtypeConstants fuse.Errno))


(def errors {:fs-getdir Errno/ENOTDIR :fs-getattr Errno/ENOENT :fs-open Errno/ENOENT :fs-read Errno/EBADF :fs-release Errno/EBADF :fs-flush Errno/EBADF})

(defmacro def-fs-fn [name args success body]
  `(defn ~name ~(into ['this] args)
    (if ~success (do ~body (identity 0)) (errors ~(clojure.lang.Keyword/intern name)))
    ))

(gen-class
  :name com.narkisr.fake
  :implements [fuse.Filesystem3]
  :prefix "fs-")

(def-fs-fn fs-getdir [path filler] (directory? (lookup path))
  (let [node (lookup path) type-to-const {:directory FuseFtypeConstants/TYPE_DIR :file FuseFtypeConstants/TYPE_FILE :link FuseFtypeConstants/TYPE_SYMLINK}]
    (doseq [child (-> node :files vals) :let [ftype (type-to-const (type child))] :when ftype]
      (. filler add (child :name) (. child hashCode) (bit-or ftype (child :mode))))))


(def NAME_LENGTH 1024)
(def BLOCK_SIZE 512)

(defn- apply-attr [setter node type length]
  (let [time (/ (System/currentTimeMillis) 1000)]
    (. setter set (. node hashCode)
      (bit-or type (node :mode)) 1 0 0 0
      length (/ (+ length (- BLOCK_SIZE 1)) BLOCK_SIZE) time time time)))

(def-fs-fn fs-getattr [path setter] (identity true)
  (let [node (lookup path)]
    (condp = (type node)
      :directory (apply-attr setter node FuseFtypeConstants/TYPE_DIR (* (-> node :files (. size)) NAME_LENGTH)) ; TODO change size to clojure idioum
      :file (apply-attr setter node FuseFtypeConstants/TYPE_FILE (-> node :content alength))
      :link (apply-attr setter node FuseFtypeConstants/TYPE_SYMLINK (-> node :link (. size)))
      Errno/ENOENT
      )))

(def-fs-fn fs-open [path flags openSetter] true
  (let [node (lookup path)]
    (. openSetter setFh (create-handle {:node node}))))

(def-fs-fn fs-read [path fh buf offset] (= :filehandle (type fh))
  (let [file (-> fh meta :node)]
    (. buf put (file :content) offset (min (. buf remaining) (- (-> file :content alength) offset)))))


(def-fs-fn fs-flush [path fh] (= :filehandle (type fh))
  (identity 0)
  )

(def-fs-fn fs-release [path fh flags] (= :filehandle (type fh))
  (System/runFinalization))
