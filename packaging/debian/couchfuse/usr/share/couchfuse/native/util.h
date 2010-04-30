#ifndef _UTIL_H_
#define _UTIL_H_

#include "javafs.h"
#include "javafs_bindings.h"


typedef struct _jfuse_params
{
   char *fuseArgv[100];
   char *javaArgv[100];
   char *filesystemClassName;
   int fuseArgc;
   int javaArgc;

} jfuse_params;


extern JavaVM *vm;
extern JNIEnv *mainEnv;

extern jobject threadGroup;
extern jobject fuseFS;

extern jclass_fuse_FuseContext       *FuseContext;
extern jclass_fuse_PasswordEntry     *PasswordEntry;
extern jclass_fuse_FuseGetattr       *FuseGetattr;
extern jclass_fuse_FuseFS            *FuseFS;
extern jclass_fuse_FuseFSDirEnt      *FuseFSDirEnt;
extern jclass_fuse_FuseFSDirFiller   *FuseFSDirFiller;
extern jclass_fuse_FuseFSFactory     *FuseFSFactory;
extern jclass_fuse_FuseOpen          *FuseOpen;
extern jclass_fuse_FuseSize          *FuseSize;
extern jclass_fuse_FuseStatfs        *FuseStatfs;
extern jclass_java_nio_ByteBuffer    *ByteBuffer;


int init_java(jfuse_params *params);
void shutdown_java();

JNIEnv * alloc_JVM(int argc, char *argv[]);
void     free_JVM(JNIEnv *env);

int      alloc_classes(JNIEnv *env);
void     free_classes(JNIEnv *env);

int      retain_threadGroup(JNIEnv *env, jobject util);
void     free_threadGroup(JNIEnv *env);

int      alloc_fuseFS(JNIEnv *env, char *filesystemClassName);
int      retain_fuseFS(JNIEnv *env, jobject util);
void     free_fuseFS(JNIEnv *env);

JNIEnv * get_env();
void     release_env(JNIEnv *env);

jint     exception_check_jerrno(JNIEnv *env, jint *jerrno);

void     create_file_handle(struct fuse_file_info *ffi, jobject ob);
jobject  read_file_handle(struct fuse_file_info *ffi);

#endif

