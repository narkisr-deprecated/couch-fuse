#include "util.h"
#include "native_impl.h"

JavaVM *vm;
JNIEnv *mainEnv;

jobject threadGroup = NULL;
jobject fuseFS = NULL;

jclass_fuse_PasswordEntry     *PasswordEntry;
jclass_fuse_FuseContext       *FuseContext;
jclass_fuse_FuseGetattr       *FuseGetattr;
jclass_fuse_FuseFS            *FuseFS;
jclass_fuse_FuseFSDirEnt      *FuseFSDirEnt;
jclass_fuse_FuseFSDirFiller   *FuseFSDirFiller;
jclass_fuse_FuseFSFactory     *FuseFSFactory;
jclass_fuse_FuseOpen          *FuseOpen;
jclass_fuse_FuseSize          *FuseSize;
jclass_fuse_FuseStatfs        *FuseStatfs;
jclass_java_nio_ByteBuffer    *ByteBuffer;


int init_java(jfuse_params *params)
{
    JNIEnv *env = NULL;

    while(1) {
        if ((env = alloc_JVM(params->javaArgc, params->javaArgv)) == NULL) break;

        if (! alloc_classes(env)) break;

        if (! alloc_fuseFS(env, params->filesystemClassName)) break;

        if (! RegisterNativeMethods(env)) break;

        return 1;
    }

    return 0;
}

void shutdown_java()
{
    JNIEnv *env = get_env();

    free_fuseFS(env);
    free_classes(env);
    free_JVM(env);
}

JNIEnv *alloc_JVM(int argc, char *argv[])
{
   JavaVMInitArgs vm_args;
   int n = argc + 3;
   JavaVMOption options[n];
   int i;
   jint res;

   for (i = 0; i < argc; i++)
      options[i].optionString = argv[i];

   /* options[i++].optionString = "-verbose:jni";                        print JNI-related messages */
   options[i++].optionString = "-Xint";
   options[i++].optionString = "-Xrs";
   options[i++].optionString = "-Xcheck:jni";

   vm_args.version = JNI_VERSION_1_4;
   vm_args.options = options;
   vm_args.nOptions = n;
   vm_args.ignoreUnrecognized = 0;

   res = JNI_CreateJavaVM(&vm, (void **)&mainEnv, &vm_args);
   if (res < 0)
   {
      WARN("Can't create Java VM");
      return NULL;
   }

   TRACE("created JVM @ %p", vm);

   return mainEnv;
}

void free_JVM(JNIEnv *env)
{
   if (vm != NULL)
   {
      (*vm)->DestroyJavaVM(vm);
      vm = NULL;
      mainEnv = NULL;
   }
}

int alloc_classes(JNIEnv *env)
{
   while (1)
   {
      if (!(FuseGetattr     = alloc_jclass_fuse_FuseGetattr(env))) break;
      if (!(FuseFSDirEnt    = alloc_jclass_fuse_FuseFSDirEnt(env))) break;
      if (!(FuseFSDirFiller = alloc_jclass_fuse_FuseFSDirFiller(env))) break;
      if (!(FuseStatfs      = alloc_jclass_fuse_FuseStatfs(env))) break;
      if (!(FuseOpen        = alloc_jclass_fuse_FuseOpen(env))) break;
      if (!(FuseSize        = alloc_jclass_fuse_FuseSize(env))) break;
      if (!(FuseContext     = alloc_jclass_fuse_FuseContext(env))) break;
      if (!(PasswordEntry   = alloc_jclass_fuse_PasswordEntry(env))) break;
      if (!(ByteBuffer      = alloc_jclass_java_nio_ByteBuffer(env))) break;
      if (!(FuseFS          = alloc_jclass_fuse_FuseFS(env))) break;
      if (!(FuseFSFactory   = alloc_jclass_fuse_FuseFSFactory(env))) break;

      return 1;
   }

   // error handler

   if ((*env)->ExceptionCheck(env))
   {
      (*env)->ExceptionDescribe(env);
   }

   free_classes(env);

   return 0;
}

void free_classes(JNIEnv *env)
{
    if (FuseGetattr != NULL)     { free_jclass_fuse_FuseGetattr(env, FuseGetattr);         FuseGetattr = NULL; }
    if (FuseFSDirEnt != NULL)    { free_jclass_fuse_FuseFSDirEnt(env, FuseFSDirEnt);       FuseFSDirEnt = NULL; }
    if (FuseFSDirFiller != NULL) { free_jclass_fuse_FuseFSDirFiller(env, FuseFSDirFiller); FuseFSDirFiller = NULL; }
    if (FuseStatfs != NULL)      { free_jclass_fuse_FuseStatfs(env, FuseStatfs);           FuseStatfs = NULL; }
    if (FuseOpen != NULL)        { free_jclass_fuse_FuseOpen(env, FuseOpen);               FuseOpen = NULL; }
    if (FuseSize != NULL)        { free_jclass_fuse_FuseSize(env, FuseSize);               FuseSize = NULL; }
    if (FuseContext != NULL)     { free_jclass_fuse_FuseContext(env, FuseContext);         FuseContext = NULL; }
    if (PasswordEntry != NULL)   { free_jclass_fuse_PasswordEntry(env, PasswordEntry);     PasswordEntry = NULL; }
    if (ByteBuffer != NULL)      { free_jclass_java_nio_ByteBuffer(env, ByteBuffer);       ByteBuffer = NULL; }
    if (FuseFS != NULL)          { free_jclass_fuse_FuseFS(env, FuseFS);                   FuseFS = NULL; }
    if (FuseFSFactory != NULL)   { free_jclass_fuse_FuseFSFactory(env, FuseFSFactory);     FuseFSFactory = NULL; }

    if ((*env)->ExceptionCheck(env))
        (*env)->ExceptionClear(env);
}

int retain_threadGroup(JNIEnv *env, jobject util)
{
   threadGroup = (*env)->NewGlobalRef(env, util);

   if ((*env)->ExceptionCheck(env))
   {
      (*env)->ExceptionDescribe(env);
      return 0;
   }

   return 1;
}

void free_threadGroup(JNIEnv *env)
{
   if (threadGroup != NULL) { (*env)->DeleteGlobalRef(env, threadGroup); threadGroup = NULL; }
}

int alloc_fuseFS(JNIEnv *env, char *filesystemClassName)
{
    jclass userFSClass = NULL;
    jmethodID userFSConstructorID;
    jobject userFSObject = NULL;
    jobject fsObject = NULL;

    while (1)
    {
        userFSClass = (*env)->FindClass(env, filesystemClassName);
        if ((*env)->ExceptionCheck(env)) break;

        userFSConstructorID = (*env)->GetMethodID(env, userFSClass, "<init>", "()V");
        if ((*env)->ExceptionCheck(env)) break;

        userFSObject = (*env)->NewObject(env, userFSClass, userFSConstructorID);
        if ((*env)->ExceptionCheck(env)) break;

        fsObject = (*env)->CallStaticObjectMethod(env, FuseFSFactory->class, FuseFSFactory->static_method.adapt__Ljava_lang_Object_, userFSObject);
        if ((*env)->ExceptionCheck(env)) break;

        fuseFS = (*env)->NewGlobalRef(env, fsObject);
        if ((*env)->ExceptionCheck(env)) break;

        TRACE("FILE SYSTEM ALLOC OK");

        return 1; /* success */
    }

   TRACE("FILE SYSTEM ALLOC FAILED");

   // error handler

   if ((*env)->ExceptionCheck(env))
   {
      (*env)->ExceptionDescribe(env);
   }

   free_fuseFS(env);

   if (fsObject     != NULL) (*env)->DeleteLocalRef(env, fsObject);
   if (userFSObject != NULL) (*env)->DeleteLocalRef(env, userFSObject);
   if (userFSClass  != NULL) (*env)->DeleteLocalRef(env, userFSClass);

   return 0;
}

int retain_fuseFS(JNIEnv *env, jobject util)
{
   fuseFS = (*env)->NewGlobalRef(env, util);

   if ((*env)->ExceptionCheck(env))
   {
      (*env)->ExceptionDescribe(env);
      return 0;
   }

   return 1;
}

void free_fuseFS(JNIEnv *env)
{
   if (fuseFS != NULL) { (*env)->DeleteGlobalRef(env, fuseFS); fuseFS = NULL; }
}

JNIEnv *get_env()
{
   JNIEnv *env;
   JavaVMAttachArgs args;

   args.version = JNI_VERSION_1_4;
   args.name = NULL;
   args.group = threadGroup;

   // a GCJ 4.0 bug workarround (supplied by Alexander Bostršm <abo@stacken.kth.se>)
   if ((*vm)->GetEnv(vm, (void**)&env, args.version) == JNI_OK)
      return env;

   TRACE("will attach thread");

   // attach thread as daemon thread so that JVM can exit after unmounting the fuseFS
   (*vm)->AttachCurrentThreadAsDaemon(vm, (void**)&env, (void*)&args);

   TRACE("did attach thread to env: %p", env);

   return env;
}

void release_env(JNIEnv *env)
{
   if (env == mainEnv)
   {
      TRACE("will NOT detach main thread from env: %p", env);
   }
   else
   {
      TRACE("will NOT detach thread from env: %p", env);

      // Currently native threads are attached to JVM as daemon threads so we don't need to
      // detach them at return from a Java method call. It is in fact beter not to detach them
      // since then every new Java call would need to atach new Java Thread until max. number of
      // Java threads is exhausted.
      //
      //TRACE("will detach thread from env: %p", env);
      //(*vm)->DetachCurrentThread(vm);
      //TRACE("did detach thread");
   }
}

jint exception_check_jerrno(JNIEnv *env, jint *jerrno)
{
   if ((*env)->ExceptionCheck(env))
   {
      (*env)->ExceptionDescribe(env);
      (*env)->ExceptionClear(env);
      if (*jerrno == 0)
      {
         *jerrno = EFAULT;
      }
   }

   return *jerrno;
}

void create_file_handle(struct fuse_file_info *ffi, jobject ob)
{
    ffi->fh = (uint64_t) ob;
}

jobject read_file_handle(struct fuse_file_info *ffi)
{
    return (jobject) ffi->fh;
}
