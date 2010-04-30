/**
 *   FUSE-J: Java bindings for FUSE (Filesystem in Userspace by Miklos Szeredi (mszeredi@inf.bme.hu))
 *
 *   Copyright (C) 2003 Peter Levart (peter@select-tech.si)
 *
 *   This program can be distributed under the terms of the GNU LGPL.
 *   See the file COPYING.LIB
 */

#include "fuse_callback.h"
#include "native_impl.h"
#include "util.h"

//
// javafs API functions

static int javafs_getattr(const char *path, struct stat *stbuf)
{
   JNIEnv *env = get_env();
   jobject jPath = NULL;
   jobject jGetattr = NULL;
   jint jerrno = 0;

   while (1)
   {
      jPath = (*env)->NewDirectByteBuffer(env, (void *)path, (jlong)strlen(path));
      if (exception_check_jerrno(env, &jerrno)) break;

      jGetattr = (*env)->NewObject(env, FuseGetattr->class, FuseGetattr->constructor.new);
      if (exception_check_jerrno(env, &jerrno)) break;

      jerrno = (*env)->CallIntMethod(env, fuseFS, FuseFS->method.getattr__Ljava_nio_ByteBuffer_Lfuse_FuseGetattrSetter_, jPath, jGetattr);
      if (exception_check_jerrno(env, &jerrno)) break;

      // inode support fix by Edwin Olson <eolson@mit.edu>
      stbuf->st_ino =    (ino_t)((*env)->GetLongField(env, jGetattr, FuseGetattr->field.inode));
      stbuf->st_mode =   (mode_t)((*env)->GetIntField(env, jGetattr, FuseGetattr->field.mode));
      stbuf->st_nlink =  (nlink_t)((*env)->GetIntField(env, jGetattr, FuseGetattr->field.nlink));
      stbuf->st_uid =    (uid_t)((*env)->GetIntField(env, jGetattr, FuseGetattr->field.uid));
      stbuf->st_gid =    (gid_t)((*env)->GetIntField(env, jGetattr, FuseGetattr->field.gid));
      stbuf->st_rdev =   (dev_t)((*env)->GetIntField(env, jGetattr, FuseGetattr->field.rdev));
      stbuf->st_size =   (off_t)((*env)->GetLongField(env, jGetattr, FuseGetattr->field.size));
      stbuf->st_blocks = (blkcnt_t)((*env)->GetLongField(env, jGetattr, FuseGetattr->field.blocks));
      stbuf->st_atime =  (time_t)((*env)->GetIntField(env, jGetattr, FuseGetattr->field.atime));
      stbuf->st_mtime =  (time_t)((*env)->GetIntField(env, jGetattr, FuseGetattr->field.mtime));
      stbuf->st_ctime =  (time_t)((*env)->GetIntField(env, jGetattr, FuseGetattr->field.ctime));

      break;
   }

   // cleanup

   if (jPath != NULL) (*env)->DeleteLocalRef(env, jPath);
   if (jGetattr != NULL) (*env)->DeleteLocalRef(env, jGetattr);

   release_env(env);

   return -jerrno;
}


static int javafs_readlink(const char *path, char *buf, size_t size)
{
   JNIEnv *env = get_env();
   jobject jPath = NULL;
   jobject jLink = NULL;
   jint jLinkPosition;
   jint jerrno = 0;

   while (1)
   {
      jPath = (*env)->NewDirectByteBuffer(env, (void *)path, (jlong)strlen(path));
      if (exception_check_jerrno(env, &jerrno)) break;

      jLink = (*env)->NewDirectByteBuffer(env, buf, (jlong)(size - 1));
      if (exception_check_jerrno(env, &jerrno)) break;

      jerrno = (*env)->CallIntMethod(env, fuseFS, FuseFS->method.readlink__Ljava_nio_ByteBuffer_Ljava_nio_ByteBuffer_, jPath, jLink);
      if (exception_check_jerrno(env, &jerrno)) break;

      // write a cstring terminator at the end of writen data
      jLinkPosition = (*env)->CallIntMethod(env, jLink, ByteBuffer->method.position);
      if (exception_check_jerrno(env, &jerrno)) break;
      buf[jLinkPosition] = '\0';

      break;
   }

   // cleanup

   if (jPath != NULL) (*env)->DeleteLocalRef(env, jPath);
   if (jLink != NULL) (*env)->DeleteLocalRef(env, jLink);

   release_env(env);

   return -jerrno;
}


static int javafs_getdir(const char *path, fuse_dirh_t h, fuse_dirfil_t filler)
{
   JNIEnv *env = get_env();
   jobject jPath = NULL;
   jobjectArray jDirEntList = NULL;
   jobject jDirEnt = NULL;
   jbyteArray jName = NULL;
   jbyte *jNameBytes = NULL;
   jsize jNameLength;
   jint mode;
   jlong inode;
   jint jerrno = 0;
   jint i;
   jint n;
   int res1;

   while (1)
   {
      jPath = (*env)->NewDirectByteBuffer(env, (void *)path, (jlong)strlen(path));
      if (exception_check_jerrno(env, &jerrno)) break;

      jDirEntList = (*env)->NewObject(env, FuseFSDirFiller->class, FuseFSDirFiller->constructor.new);
      if (exception_check_jerrno(env, &jerrno)) break;

      jerrno = (*env)->CallIntMethod(env, fuseFS, FuseFS->method.getdir__Ljava_nio_ByteBuffer_Lfuse_FuseFSDirFiller_, jPath, jDirEntList);
      if (exception_check_jerrno(env, &jerrno)) break;

      n = (*env)->CallIntMethod(env, jDirEntList, FuseFSDirFiller->method.size);
      if (exception_check_jerrno(env, &jerrno)) break;

      for (i = 0; i < n; i++)
      {
         char name[MAX_GETDIR_NAME_LENGTH];
         int nameLength = MAX_GETDIR_NAME_LENGTH - 1;

         jDirEnt = (*env)->CallObjectMethod(env, jDirEntList, FuseFSDirFiller->method.get__I, i);
         if (exception_check_jerrno(env, &jerrno)) break;

         mode = (*env)->GetIntField(env, jDirEnt, FuseFSDirEnt->field.mode);
         if (exception_check_jerrno(env, &jerrno)) break;

         jName = (*env)->GetObjectField(env, jDirEnt, FuseFSDirEnt->field.name);
         if (exception_check_jerrno(env, &jerrno)) break;

         inode = (*env)->GetLongField(env, jDirEnt, FuseFSDirEnt->field.inode);
         if (exception_check_jerrno(env, &jerrno)) break;

         jNameBytes = (*env)->GetByteArrayElements(env, jName, NULL);
         if (exception_check_jerrno(env, &jerrno)) break;

         jNameLength = (*env)->GetArrayLength(env, jName);

         if (nameLength > (int)jNameLength)
            nameLength = (int)jNameLength;

         memcpy(name, jNameBytes, nameLength);
         name[nameLength] = '\0';

         res1 = filler(h, name, IFTODT(mode), inode);
         if (res1 != 0)
         {
            jerrno = (jint) res1;
            break;
         }

         (*env)->ReleaseByteArrayElements(env, jName, jNameBytes, JNI_ABORT); jNameBytes = NULL;
         (*env)->DeleteLocalRef(env, jName); jName = NULL;
         (*env)->DeleteLocalRef(env, jDirEnt); jDirEnt = NULL;
      }

      break;
   }

   // cleanup

   if (jNameBytes != NULL) (*env)->ReleaseByteArrayElements(env, jName, jNameBytes, JNI_ABORT);
   if (jName != NULL) (*env)->DeleteLocalRef(env, jName);
   if (jDirEnt != NULL) (*env)->DeleteLocalRef(env, jDirEnt);
   if (jDirEntList != NULL) (*env)->DeleteLocalRef(env, jDirEntList);
   if (jPath != NULL) (*env)->DeleteLocalRef(env, jPath);

   release_env(env);

   return -jerrno;
}


static int javafs_mknod(const char *path, mode_t mode, dev_t rdev)
{
   JNIEnv *env = get_env();
   jobject jPath = NULL;
   jint jerrno = 0;

   while (1)
   {
      jPath = (*env)->NewDirectByteBuffer(env, (void *)path, (jlong)strlen(path));
      if (exception_check_jerrno(env, &jerrno)) break;

      jerrno = (*env)->CallIntMethod(env, fuseFS, FuseFS->method.mknod__Ljava_nio_ByteBuffer_II, jPath, (jint)mode, (jint)rdev);
      exception_check_jerrno(env, &jerrno);
      break;
   }

   // cleanup

   if (jPath != NULL) (*env)->DeleteLocalRef(env, jPath);

   release_env(env);

   return -jerrno;
}


static int javafs_mkdir(const char *path, mode_t mode)
{
   JNIEnv *env = get_env();
   jobject jPath = NULL;
   jint jerrno = 0;

   while (1)
   {
      jPath = (*env)->NewDirectByteBuffer(env, (void *)path, (jlong)strlen(path));
      if (exception_check_jerrno(env, &jerrno)) break;

      jerrno = (*env)->CallIntMethod(env, fuseFS, FuseFS->method.mkdir__Ljava_nio_ByteBuffer_I, jPath, (jint)mode);
      exception_check_jerrno(env, &jerrno);
      break;
   }

   // cleanup

   if (jPath != NULL) (*env)->DeleteLocalRef(env, jPath);

   release_env(env);

   return -jerrno;
}


static int javafs_unlink(const char *path)
{
   JNIEnv *env = get_env();
   jobject jPath = NULL;
   jint jerrno = 0;

   while (1)
   {
      jPath = (*env)->NewDirectByteBuffer(env, (void *)path, (jlong)strlen(path));
      if (exception_check_jerrno(env, &jerrno)) break;

      jerrno = (*env)->CallIntMethod(env, fuseFS, FuseFS->method.unlink__Ljava_nio_ByteBuffer_, jPath);
      exception_check_jerrno(env, &jerrno);
      break;
   }

   // cleanup

   if (jPath != NULL) (*env)->DeleteLocalRef(env, jPath);

   release_env(env);

   return -jerrno;
}


static int javafs_rmdir(const char *path)
{
   JNIEnv *env = get_env();
   jobject jPath = NULL;
   jint jerrno = 0;

   while (1)
   {
      jPath = (*env)->NewDirectByteBuffer(env, (void *)path, (jlong)strlen(path));
      if (exception_check_jerrno(env, &jerrno)) break;

      jerrno = (*env)->CallIntMethod(env, fuseFS, FuseFS->method.rmdir__Ljava_nio_ByteBuffer_, jPath);
      exception_check_jerrno(env, &jerrno);
      break;
   }

   // cleanup

   if (jPath != NULL) (*env)->DeleteLocalRef(env, jPath);

   release_env(env);

   return -jerrno;
}


static int javafs_symlink(const char *from, const char *to)
{
   JNIEnv *env = get_env();
   jobject jFrom = NULL;
   jobject jTo = NULL;
   jint jerrno = 0;

   while (1)
   {
      jFrom = (*env)->NewDirectByteBuffer(env, (void *)from, (jlong)strlen(from));
      if (exception_check_jerrno(env, &jerrno)) break;

      jTo = (*env)->NewDirectByteBuffer(env, (void *)to, (jlong)strlen(to));
      if (exception_check_jerrno(env, &jerrno)) break;

      jerrno = (*env)->CallIntMethod(env, fuseFS, FuseFS->method.symlink__Ljava_nio_ByteBuffer_Ljava_nio_ByteBuffer_, jFrom, jTo);
      exception_check_jerrno(env, &jerrno);
      break;
   }

   // cleanup

   if (jTo != NULL) (*env)->DeleteLocalRef(env, jTo);
   if (jFrom != NULL) (*env)->DeleteLocalRef(env, jFrom);

   release_env(env);

   return -jerrno;
}


static int javafs_rename(const char *from, const char *to)
{
   JNIEnv *env = get_env();
   jobject jFrom = NULL;
   jobject jTo = NULL;
   jint jerrno = 0;

   while (1)
   {
      jFrom = (*env)->NewDirectByteBuffer(env, (void *)from, (jlong)strlen(from));
      if (exception_check_jerrno(env, &jerrno)) break;

      jTo = (*env)->NewDirectByteBuffer(env, (void *)to, (jlong)strlen(to));
      if (exception_check_jerrno(env, &jerrno)) break;

      jerrno = (*env)->CallIntMethod(env, fuseFS, FuseFS->method.rename__Ljava_nio_ByteBuffer_Ljava_nio_ByteBuffer_, jFrom, jTo);
      exception_check_jerrno(env, &jerrno);
      break;
   }

   // cleanup

   if (jTo != NULL) (*env)->DeleteLocalRef(env, jTo);
   if (jFrom != NULL) (*env)->DeleteLocalRef(env, jFrom);

   release_env(env);

   return -jerrno;
}


static int javafs_link(const char *from, const char *to)
{
   JNIEnv *env = get_env();
   jobject jFrom = NULL;
   jobject jTo = NULL;
   jint jerrno = 0;

   while (1)
   {
      jFrom = (*env)->NewDirectByteBuffer(env, (void *)from, (jlong)strlen(from));
      if (exception_check_jerrno(env, &jerrno)) break;

      jTo = (*env)->NewDirectByteBuffer(env, (void *)to, (jlong)strlen(to));
      if (exception_check_jerrno(env, &jerrno)) break;

      jerrno = (*env)->CallIntMethod(env, fuseFS, FuseFS->method.link__Ljava_nio_ByteBuffer_Ljava_nio_ByteBuffer_, jFrom, jTo);
      exception_check_jerrno(env, &jerrno);
      break;
   }

   // cleanup

   if (jTo != NULL) (*env)->DeleteLocalRef(env, jTo);
   if (jFrom != NULL) (*env)->DeleteLocalRef(env, jFrom);

   release_env(env);

   return -jerrno;
}


static int javafs_chmod(const char *path, mode_t mode)
{
   JNIEnv *env = get_env();
   jobject jPath = NULL;
   jint jerrno = 0;

   while (1)
   {
      jPath = (*env)->NewDirectByteBuffer(env, (void *)path, (jlong)strlen(path));
      if (exception_check_jerrno(env, &jerrno)) break;

      jerrno = (*env)->CallIntMethod(env, fuseFS, FuseFS->method.chmod__Ljava_nio_ByteBuffer_I, jPath, (jint)mode);
      exception_check_jerrno(env, &jerrno);
      break;
   }

   // cleanup

   if (jPath != NULL) (*env)->DeleteLocalRef(env, jPath);

   release_env(env);

   return -jerrno;
}


static int javafs_chown(const char *path, uid_t uid, gid_t gid)
{
   JNIEnv *env = get_env();
   jobject jPath = NULL;
   jint jerrno = 0;

   while (1)
   {
      jPath = (*env)->NewDirectByteBuffer(env, (void *)path, (jlong)strlen(path));
      if (exception_check_jerrno(env, &jerrno)) break;

      jerrno = (*env)->CallIntMethod(env, fuseFS, FuseFS->method.chown__Ljava_nio_ByteBuffer_II, jPath, (jint)uid, (jint)gid);
      exception_check_jerrno(env, &jerrno);
      break;
   }

   // cleanup

   if (jPath != NULL) (*env)->DeleteLocalRef(env, jPath);

   release_env(env);

   return -jerrno;
}


static int javafs_truncate(const char *path, off_t size)
{
   JNIEnv *env = get_env();
   jobject jPath = NULL;
   jint jerrno = 0;

   while (1)
   {
      jPath = (*env)->NewDirectByteBuffer(env, (void *)path, (jlong)strlen(path));
      if (exception_check_jerrno(env, &jerrno)) break;

      jerrno = (*env)->CallIntMethod(env, fuseFS, FuseFS->method.truncate__Ljava_nio_ByteBuffer_J, jPath, (jlong)size);
      exception_check_jerrno(env, &jerrno);
      break;
   }

   // cleanup

   if (jPath != NULL) (*env)->DeleteLocalRef(env, jPath);

   release_env(env);

   return -jerrno;
}


static int javafs_utime(const char *path, struct utimbuf *buf)
{
   JNIEnv *env = get_env();
   jobject jPath = NULL;
   jint jerrno = 0;

   while (1)
   {
      jPath = (*env)->NewDirectByteBuffer(env, (void *)path, (jlong)strlen(path));
      if (exception_check_jerrno(env, &jerrno)) break;

      (*env)->CallVoidMethod(env, fuseFS, FuseFS->method.utime__Ljava_nio_ByteBuffer_II, jPath, (jint)(buf->actime), (jint)(buf->modtime));
      exception_check_jerrno(env, &jerrno);
      break;
   }

   // cleanup

   if (jPath != NULL) (*env)->DeleteLocalRef(env, jPath);

   release_env(env);

   return -jerrno;
}


static int javafs_open(const char *path, struct fuse_file_info *ffi)
{
   JNIEnv *env = get_env();
   jobject jPath = NULL;
   jobject jOpen = NULL;
   jint jerrno = 0;

   while (1)
   {
      jPath = (*env)->NewDirectByteBuffer(env, (void *)path, (jlong)strlen(path));
      if (exception_check_jerrno(env, &jerrno)) break;

      jOpen = (*env)->NewObject(env, FuseOpen->class, FuseOpen->constructor.new);
      if (exception_check_jerrno(env, &jerrno)) break;

      (*env)->SetBooleanField(env, jOpen, FuseOpen->field.directIO, ffi->direct_io ? JNI_TRUE : JNI_FALSE);
      (*env)->SetBooleanField(env, jOpen, FuseOpen->field.keepCache, ffi->keep_cache ? JNI_TRUE : JNI_FALSE);

      jerrno = (*env)->CallIntMethod(env, fuseFS, FuseFS->method.open__Ljava_nio_ByteBuffer_ILfuse_FuseOpenSetter_, jPath, (jint)(ffi->flags), jOpen);
      if (exception_check_jerrno(env, &jerrno)) break;

      // if fh is non null then create a global reference to it (will be released in release callback)
      jobject jFh = (*env)->GetObjectField(env, jOpen, FuseOpen->field.fh);
      jobject jFhGlobalRef = (jFh == NULL) ? NULL : (*env)->NewGlobalRef(env, jFh);

      // every sane platform should store a pointer into unsigned long without a problem
      create_file_handle(ffi, jFhGlobalRef);
      ffi->direct_io = ((*env)->GetBooleanField(env, jOpen, FuseOpen->field.directIO) == JNI_TRUE)? 1 : 0;
      ffi->keep_cache = ((*env)->GetBooleanField(env, jOpen, FuseOpen->field.keepCache) == JNI_TRUE)? 1 : 0;

      // remove local reference to fh
      if (jFh != NULL) (*env)->DeleteLocalRef(env, jFh);

      break;
   }

   // cleanup

   if (jPath != NULL) (*env)->DeleteLocalRef(env, jPath);
   if (jOpen != NULL) (*env)->DeleteLocalRef(env, jOpen);

   release_env(env);

   return -jerrno;
}


static int javafs_read(const char *path, char *buf, size_t size, off_t offset, struct fuse_file_info *ffi)
{
   JNIEnv *env = get_env();
   jobject jPath = NULL;
   jobject jBuf = NULL;
   jint jerrno = 0;
   jint nread = 0;

   while (1)
   {
      jPath = (*env)->NewDirectByteBuffer(env, (void *)path, (jlong)strlen(path));
      if (exception_check_jerrno(env, &jerrno)) break;

      jBuf = (*env)->NewDirectByteBuffer(env, buf, (jlong)size);
      if (exception_check_jerrno(env, &jerrno)) break;

      jerrno = (*env)->CallIntMethod(env, fuseFS, FuseFS->method.read__Ljava_nio_ByteBuffer_Ljava_lang_Object_Ljava_nio_ByteBuffer_J, jPath, read_file_handle(ffi), jBuf, (jlong)offset);
      if (exception_check_jerrno(env, &jerrno)) break;

      // to obtain # of bytes read, get current position from ByteBuffer
      nread = (*env)->CallIntMethod(env, jBuf, ByteBuffer->method.position);
      exception_check_jerrno(env, &jerrno);
      break;
   }

   // cleanup

   if (jBuf != NULL) (*env)->DeleteLocalRef(env, jBuf);
   if (jPath != NULL) (*env)->DeleteLocalRef(env, jPath);

   release_env(env);

   return jerrno? -jerrno : nread;
}


static int javafs_write(const char *path, const char *buf, size_t size, off_t offset, struct fuse_file_info *ffi)
{
   JNIEnv *env = get_env();
   jobject jPath = NULL;
   jobject jBuf = NULL;
   jint jerrno = 0;
   jint nwriten = 0;

   while (1)
   {
      jPath = (*env)->NewDirectByteBuffer(env, (void *)path, (jlong)strlen(path));
      if (exception_check_jerrno(env, &jerrno)) break;

      jBuf = (*env)->NewDirectByteBuffer(env, (void *)buf, (jlong)size);
      if (exception_check_jerrno(env, &jerrno)) break;

      jerrno = (*env)->CallIntMethod(env, fuseFS, FuseFS->method.write__Ljava_nio_ByteBuffer_Ljava_lang_Object_ZLjava_nio_ByteBuffer_J, jPath, read_file_handle(ffi), (ffi->writepage)? JNI_TRUE : JNI_FALSE, jBuf, (jlong)offset);
      if (exception_check_jerrno(env, &jerrno)) break;

      // to obtain # of bytes writen, get current position from ByteBuffer
      nwriten = (*env)->CallIntMethod(env, jBuf, ByteBuffer->method.position);
      exception_check_jerrno(env, &jerrno);
      break;
   }

   // cleanup

   if (jBuf != NULL) (*env)->DeleteLocalRef(env, jBuf);
   if (jPath != NULL) (*env)->DeleteLocalRef(env, jPath);

   release_env(env);

   return jerrno? -jerrno : nwriten;
}


static int javafs_statvfs(const char *path, struct statvfs *fst)
{
   JNIEnv *env = get_env();
   jobject jStatfs = NULL;
   jint jerrno = 0;

   while (1)
   {
      jStatfs = (*env)->NewObject(env, FuseStatfs->class, FuseStatfs->constructor.new);
      if (exception_check_jerrno(env, &jerrno)) break;

      jerrno = (*env)->CallIntMethod(env, fuseFS, FuseFS->method.statfs__Lfuse_FuseStatfsSetter_, jStatfs);
      if (exception_check_jerrno(env, &jerrno)) break;

      fst->f_bsize   = (long) (*env)->GetIntField(env, jStatfs, FuseStatfs->field.blockSize);
      fst->f_blocks  = (long) (*env)->GetIntField(env, jStatfs, FuseStatfs->field.blocks);
      fst->f_bfree   = (long) (*env)->GetIntField(env, jStatfs, FuseStatfs->field.blocksFree);
      fst->f_bavail  = (long) (*env)->GetIntField(env, jStatfs, FuseStatfs->field.blocksAvail);
      fst->f_files   = (long) (*env)->GetIntField(env, jStatfs, FuseStatfs->field.files);
      fst->f_ffree   = (long) (*env)->GetIntField(env, jStatfs, FuseStatfs->field.filesFree);
      fst->f_namemax = (long) (*env)->GetIntField(env, jStatfs, FuseStatfs->field.namelen);
      break;
   }

   // cleanup

   if (jStatfs != NULL) (*env)->DeleteLocalRef(env, jStatfs);

   release_env(env);
   return -jerrno;
}


static int javafs_flush(const char *path, struct fuse_file_info *ffi)
{
   JNIEnv *env = get_env();
   jobject jPath = NULL;
   jint jerrno = 0;

   while (1)
   {
      jPath = (*env)->NewDirectByteBuffer(env, (void *)path, (jlong)strlen(path));
      if (exception_check_jerrno(env, &jerrno)) break;

      jerrno = (*env)->CallIntMethod(env, fuseFS, FuseFS->method.flush__Ljava_nio_ByteBuffer_Ljava_lang_Object_, jPath, read_file_handle(ffi));
      exception_check_jerrno(env, &jerrno);
      break;
   }

   // cleanup

   if (jPath != NULL) (*env)->DeleteLocalRef(env, jPath);

   release_env(env);

   return -jerrno;
}


static int javafs_release(const char *path, struct fuse_file_info *ffi)
{
   JNIEnv *env = get_env();
   jobject jPath = NULL;
   jobject jFh = read_file_handle(ffi);
   jint jerrno = 0;

   while (1)
   {
      jPath = (*env)->NewDirectByteBuffer(env, (void *)path, (jlong)strlen(path));
      if (exception_check_jerrno(env, &jerrno)) break;

      jerrno = (*env)->CallIntMethod(env, fuseFS, FuseFS->method.release__Ljava_nio_ByteBuffer_Ljava_lang_Object_I, jPath, jFh, (jint)(ffi->flags));
      exception_check_jerrno(env, &jerrno);
      break;
   }

   // cleanup

   if (jPath != NULL) (*env)->DeleteLocalRef(env, jPath);

   // jFh is global reference and should be released in release callback
   if (jFh != NULL)
   {
      (*env)->DeleteGlobalRef(env, jFh);
      ffi->fh = 0;
   }

   release_env(env);

   return -jerrno;
}


static int javafs_fsync(const char *path, int datasync, struct fuse_file_info *ffi)
{
   JNIEnv *env = get_env();
   jobject jPath = NULL;
   jint jerrno = 0;

   while (1)
   {
      jPath = (*env)->NewDirectByteBuffer(env, (void *)path, (jlong)strlen(path));
      if (exception_check_jerrno(env, &jerrno)) break;

      jerrno = (*env)->CallIntMethod(env, fuseFS, FuseFS->method.fsync__Ljava_nio_ByteBuffer_Ljava_lang_Object_Z, jPath, read_file_handle(ffi), (jint)(ffi->flags), datasync? JNI_TRUE : JNI_FALSE);
      exception_check_jerrno(env, &jerrno);
      break;
   }

   // cleanup

   if (jPath != NULL) (*env)->DeleteLocalRef(env, jPath);

   release_env(env);

   return -jerrno;
}


//
// extended attributes support contributed by Steven Pearson <steven_pearson@final-step.com>
// and then modified by Peter Levart <peter@select-tech.si> to fit the new errno returning scheme

static int javafs_setxattr(const char *path, const char *name, const char *value, size_t size, int flags, uint32_t position)
{
   JNIEnv *env = get_env();
   jobject jPath = NULL;
   jobject jName = NULL;
   jobject jValue = NULL;
   jint jerrno = 0;

   while(1)
   {
      jobject jPath = (*env)->NewDirectByteBuffer(env, (void *)path, (jlong)strlen(path));
      if (exception_check_jerrno(env, &jerrno)) break;

      jobject jName = (*env)->NewDirectByteBuffer(env, (void *)name, (jlong)strlen(name));
      if (exception_check_jerrno(env, &jerrno)) break;

      jobject jValue = (*env)->NewDirectByteBuffer(env, (void *)value, (jlong)size);
      if (exception_check_jerrno(env, &jerrno)) break;

      jerrno = (*env)->CallIntMethod(env, fuseFS, FuseFS->method.setxattr__Ljava_nio_ByteBuffer_Ljava_nio_ByteBuffer_Ljava_nio_ByteBuffer_II, jPath, jName, jValue, (jint)flags, (jint)position);
      exception_check_jerrno(env, &jerrno);
      break;
   }

   // cleanup

   if (jPath != NULL) (*env)->DeleteLocalRef(env, jPath);
   if (jName != NULL) (*env)->DeleteLocalRef(env, jName);
   if (jValue != NULL) (*env)->DeleteLocalRef(env, jValue);

   release_env(env);

   return -jerrno;
}

static int javafs_getxattr(const char *path, const char *name, char *value, size_t size, uint32_t position)
{
   JNIEnv *env = get_env();
   jobject jPath = NULL;
   jobject jName = NULL;
   jobject jValue = NULL;
   jobject jSize = NULL;
   jint jerrno = 0;
   jint xattrsize;

   while (1)
   {
      jPath = (*env)->NewDirectByteBuffer(env, (void *)path, (jlong)strlen(path));
      if (exception_check_jerrno(env, &jerrno)) break;

      jName = (*env)->NewDirectByteBuffer(env, (void *)name, (jlong)strlen(name));
      if (exception_check_jerrno(env, &jerrno)) break;

      // Size of the attribute
      if (size == 0)
      {
         jSize = (*env)->NewObject(env, FuseSize->class, FuseSize->constructor.new);
         if (exception_check_jerrno(env, &jerrno)) break;

         jerrno = (*env)->CallIntMethod(env, fuseFS, FuseFS->method.getxattrsize__Ljava_nio_ByteBuffer_Ljava_nio_ByteBuffer_Lfuse_FuseSizeSetter_, jPath, jName, jSize);
         if (exception_check_jerrno(env, &jerrno)) break;

         xattrsize = (*env)->GetIntField(env, jSize, FuseSize->field.size);
      }
      else
      {
         jValue = (*env)->NewDirectByteBuffer(env, value , (jlong)size);
         if (exception_check_jerrno(env, &jerrno)) break;

         jerrno = (*env)->CallIntMethod(env, fuseFS, FuseFS->method.getxattr__Ljava_nio_ByteBuffer_Ljava_nio_ByteBuffer_Ljava_nio_ByteBuffer_I, jPath, jName, jValue, (jint)position);
         if (exception_check_jerrno(env, &jerrno)) break;

         // to obtain # of bytes read, get current position from ByteBuffer
         xattrsize = (*env)->CallIntMethod(env, jValue, ByteBuffer->method.position);
      }

      exception_check_jerrno(env, &jerrno);
      break;
   }

   // cleanup

   if (jSize != NULL) (*env)->DeleteLocalRef(env, jSize);
   if (jValue != NULL) (*env)->DeleteLocalRef(env, jValue);
   if (jName != NULL) (*env)->DeleteLocalRef(env, jName);
   if (jPath != NULL) (*env)->DeleteLocalRef(env, jPath);

   release_env(env);

   return jerrno? -jerrno : xattrsize;
}

static int javafs_listxattr(const char *path, char *list, size_t size)
{
   JNIEnv *env = get_env();
   jobject jPath = NULL;
   jobject jList = NULL;
   jobject jSize = NULL;
   jint jerrno = 0;
   jint xattrsize;

   while (1)
   {
      jPath = (*env)->NewDirectByteBuffer(env, (void *)path, (jlong)strlen(path));
      if (exception_check_jerrno(env, &jerrno)) break;

      // Size of the attribute list
      if (size == 0)
      {
         jSize = (*env)->NewObject(env, FuseSize->class, FuseSize->constructor.new);
         if (exception_check_jerrno(env, &jerrno)) break;

         jerrno = (*env)->CallIntMethod(env, fuseFS, FuseFS->method.listxattrsize__Ljava_nio_ByteBuffer_Lfuse_FuseSizeSetter_, jPath, jSize);
         if (exception_check_jerrno(env, &jerrno)) break;

         xattrsize = (*env)->GetIntField(env, jSize, FuseSize->field.size);
      }
      else
      {
         jList = (*env)->NewDirectByteBuffer(env, list , (jlong)size);
         if (exception_check_jerrno(env, &jerrno)) break;

         jerrno = (*env)->CallIntMethod(env, fuseFS, FuseFS->method.listxattr__Ljava_nio_ByteBuffer_Ljava_nio_ByteBuffer_, jPath, jList);
         if (exception_check_jerrno(env, &jerrno)) break;

         // to obtain # of bytes read, get current position from ByteBuffer
         xattrsize = (*env)->CallIntMethod(env, jList, ByteBuffer->method.position);
      }

      exception_check_jerrno(env, &jerrno);
      break;
   }

   // cleanup

   if (jSize != NULL) (*env)->DeleteLocalRef(env, jSize);
   if (jPath != NULL) (*env)->DeleteLocalRef(env, jPath);
   if (jList != NULL) (*env)->DeleteLocalRef(env, jList);

   release_env(env);

   return jerrno? -jerrno : xattrsize;
}

static int javafs_removexattr(const char *path, const char *name)
{
   JNIEnv *env = get_env();
   jobject jPath = NULL;
   jobject jName = NULL;
   jint jerrno = 0;

   while (1)
   {
      jPath = (*env)->NewDirectByteBuffer(env, (void *)path, (jlong)strlen(path));
      if (exception_check_jerrno(env, &jerrno)) break;

      jName = (*env)->NewDirectByteBuffer(env, (void *)name, (jlong)strlen(name));
      if (exception_check_jerrno(env, &jerrno)) break;

      jerrno = (*env)->CallIntMethod(env, fuseFS, FuseFS->method.removexattr__Ljava_nio_ByteBuffer_Ljava_nio_ByteBuffer_, jPath, jName);
      exception_check_jerrno(env, &jerrno);
      break;
   }

   // cleanup

   if (jPath != NULL) (*env)->DeleteLocalRef(env, jPath);
   if (jName != NULL) (*env)->DeleteLocalRef(env, jName);

   release_env(env);

   return -jerrno;
}

/**
 * Initialize filesystem
 *
 * The return value will passed in the private_data field of
 * fuse_context to all file operations and as a parameter to the
 * destroy() method.
 *
 * Introduced in version 2.3
 * Changed in version 2.6
 */
static void * javafs_init(struct fuse_conn_info *conn)
{
    jfuse_params *params = fuse_get_context()->private_data;

    // The param's object is not set by the mount call
    // Hence java is only initialized if called from the launcher

    if (params)
    {
        if (init_java(params))
        {
            JNIEnv *env = get_env();
            jint jerrno = (*env)->CallIntMethod(env, fuseFS, FuseFS->method.init);
            exception_check_jerrno(env, &jerrno);
            release_env(env);

            return params;
        }
    }

    return NULL;
}

/**
 * Clean up filesystem
 *
 * Called on filesystem exit.
 *
 * Introduced in version 2.3
 */
static void javafs_destroy(void *data)
{
    // The data's object is not set by the mount call
    // Hence java is only initialized if called from the launcher

    if (data)
    {
        JNIEnv *env = get_env();
        jint jerrno = (*env)->CallIntMethod(env, fuseFS, FuseFS->method.destroy);
        exception_check_jerrno(env, &jerrno);
        release_env(env);

        shutdown_java();
    }
}


struct fuse_operations javafs_oper = {
   getattr:    javafs_getattr,
   readlink:   javafs_readlink,
   getdir:     javafs_getdir,
   mknod:      javafs_mknod,
   mkdir:      javafs_mkdir,
   symlink:    javafs_symlink,
   unlink:     javafs_unlink,
   rmdir:      javafs_rmdir,
   rename:     javafs_rename,
   link:       javafs_link,
   chmod:      javafs_chmod,
   chown:      javafs_chown,
   truncate:   javafs_truncate,
   utime:      javafs_utime,
   open:       javafs_open,
   read:       javafs_read,
   write:      javafs_write,
   statfs:     javafs_statvfs,
   flush:      javafs_flush,
   release:    javafs_release,
   fsync:      javafs_fsync,
   // extended attributes are now implemented
   setxattr:    javafs_setxattr,
   getxattr:    javafs_getxattr,
   listxattr:   javafs_listxattr,
   removexattr: javafs_removexattr,
   opendir:     NULL,
   readdir:     NULL,
   releasedir:  NULL,
   fsyncdir:    NULL,
   init:        javafs_init,
   destroy:     javafs_destroy,
   access:      NULL,
   create:      NULL,
   ftruncate:   NULL,
   fgetattr:    NULL
};
