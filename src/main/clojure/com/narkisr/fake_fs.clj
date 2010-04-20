(ns com.narkisr.fake-fs
  (:gen-class)
  (:import fuse.FuseMount org.apache.commons.logging.LogFactory))

(defn- with-type [type x]
  (with-meta x {:type type}))


(defstruct node :name :mode :xattrs)
(defstruct directory :node :files)
(defstruct file :node :content)
(defstruct link :node :link)

(def root
  (with-type :directory
    (struct directory (struct node "/" 0755 [:description "Root directory"])
      [(struct file (struct node "README" 0644 [:description "A Readme File" :mimetype "text/plain"]) (. "this is a nice readme contents" getBytes))])))



(defn lookup [path]
  (if (= path "/") root
    (let [f (java.io.File. path) parent (lookup (. f getParent))]
      (if ((type parent) :directory) (find (parent :files) (. f getName))))))

(gen-class
  :name com.narkisr.fake
  :implements [fuse.Filesystem3]
  :prefix "fs-")

(defn fs-getdir [this path dirFiller]
  (identity 0))


(defn -main []
  (FuseMount/mount
    (into-array ["/home/ronen/CodeProjects/couch-fuse/fake" "-f"])
    (new com.narkisr.fake)
    (LogFactory/getLog (class com.narkisr.fake))))


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