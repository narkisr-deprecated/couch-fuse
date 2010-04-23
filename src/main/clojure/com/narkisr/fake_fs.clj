(ns com.narkisr.fake-fs
  (:use com.narkisr.mocking com.narkisr.fs-logic)
  (:import fuse.FuseFtypeConstants fuse.Errno))


(defmacro defn-with-this [name args body]
  `(defn ~name ~(into ['this] args) (~@body)))

(gen-class
  :name com.narkisr.fake
  :implements [fuse.Filesystem3]
  :prefix "fs-")

(defn-with-this fs-getdir [path filler]
  (let [node (lookup path) type-to-const {:directory FuseFtypeConstants/TYPE_DIR :file FuseFtypeConstants/TYPE_FILE :link FuseFtypeConstants/TYPE_SYMLINK}]
    (if (directory? node)
      (do (doseq [child (-> node :files vals) :let [ftype (type-to-const (type child))] :when ftype]
        (. filler add (child :name) (. child hashCode) (bit-or ftype (child :mode))))
        (identity 0))
      (identity Errno/ENOENT))))

(fs-getdir " la " "/" (mock fuse.FuseDirFiller))

(def NAME_LENGTH 1024)
(def BLOCK_SIZE 512)

(defn- apply-attr [setter node type length]
  (let [time (/ (System/currentTimeMillis) 1000)]
    (. setter set (. node hashCode)
      (bit-or type (node :mode)) 1 0 0 0
      length (/ (+ length (- BLOCK_SIZE 1)) BLOCK_SIZE) time time time)
    (identity 0)))

(defn-with-this fs-getattr [path setter]
  (let [node (lookup path)]
    (condp = (type node)
      :directory (apply-attr setter node FuseFtypeConstants/TYPE_DIR (* (-> node :files (. size)) NAME_LENGTH)) ; TODO change size to clojure idioum
      :file (apply-attr setter node FuseFtypeConstants/TYPE_FILE (-> node :content alength))
      :link (apply-attr setter node FuseFtypeConstants/TYPE_SYMLINK (-> node :link (. size)))
      Errno/ENOENT
      )))


(comment
  public int getattr (String path, FuseGetattrSetter getattrSetter) throws FuseException ;

  public int readlink (String path, CharBuffer link) throws FuseException ;

  public int getdir (String path, FuseDirFiller dirFiller) throws FuseException ;

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