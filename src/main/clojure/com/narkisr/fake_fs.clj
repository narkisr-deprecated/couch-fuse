(ns com.narkisr.fake-fs
  (:use com.narkisr.mocking com.narkisr.fs-logic)
  (:import fuse.FuseFtypeConstants fuse.Errno))


(def errors {:fs-getdir Errno/ENOTDIR :fs-getattr Errno/ENOENT})

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

(def-fs-fn fs-getattr [path setter] true
  (let [node (lookup path)]
    (condp = (type node)
      :directory (apply-attr setter node FuseFtypeConstants/TYPE_DIR (* (-> node :files (. size)) NAME_LENGTH)) ; TODO change size to clojure idioum
      :file (apply-attr setter node FuseFtypeConstants/TYPE_FILE (-> node :content alength))
      :link (apply-attr setter node FuseFtypeConstants/TYPE_SYMLINK (-> node :link (. size)))
      )))

;(fs-getattr "this " "/" (mock fuse.FuseGetattrSetter ))

(comment

  public int readlink (String path, CharBuffer link) throws FuseException ;

  public int mknod (String path, int mode, int rdev) throws FuseException ;

  public int mkdir (String path, int mode) throws FuseException ;

  public int unlink (String path) throws FuseException ;

  public int rmdir (String path) throws FuseException ;

  public int symlink (String from, String to) throws FuseException ;

  public int rename (String from, String to) throws FuseException ;

  public int link (String from, String to) throws FuseException ;

  public int chmod (String path, int mode) throws FuseException ;

  public int chown (String path, int uid, int gid) throws FuseException ;

  public int truncate (String path, long size) throws FuseException ;

  public int utime (String path, int atime, int mtime) throws FuseException ;

  public int statfs (FuseStatfsSetter statfsSetter) throws FuseException ;//if open returns a filehandle by calling FuseOpenSetter.setFh () method, it will be passed to every method that supports 'fh 'argument
  public int open (String path, int flags, FuseOpenSetter openSetter) throws FuseException ;//fh is filehandle passed from open
  public int read (String path, Object fh, ByteBuffer buf, long offset) throws FuseException ;//fh is filehandle passed from open,//isWritepage indicates that write was caused by a writepage
  public int write (String path, Object fh, boolean isWritepage, ByteBuffer buf, long offset) throws FuseException ;//called on every filehandle close, fh is filehandle passed from open
  public int flush (String path, Object fh) throws FuseException ;//called when last filehandle is closed, fh is filehandle passed from open
  public int release (String path, Object fh, int flags) throws FuseException ;//Synchronize file contents, fh is filehandle passed from open,//isDatasync indicates that only the user data should be flushed, not the meta data
  public int fsync (String path, Object fh, boolean isDatasync) throws FuseException ;
  )