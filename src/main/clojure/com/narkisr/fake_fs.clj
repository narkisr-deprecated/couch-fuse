(ns com.narkisr.fake-fs
  (:gen-class))

(gen-class
  :name com.narkisr.fake
  :implements [fuse.Filesystem3]
  :prefix "filesystem-")

(defn -main []
  (println "hey"))

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