#include "javafs.h"
#include "fuse_callback.h"
#include "util.h"

int main(int argc, char *argv[])
{
    int i;
    jfuse_params *params = calloc(1, sizeof(jfuse_params));

    // split args into fuse & java args
    for (i = 0; i < argc; i++)
    {
        char *arg = argv[i];
        if (!strncmp(arg, "-C", 2))
         params->filesystemClassName = &(arg[2]);
        else if (!strncmp(arg, "-J", 2))
         params->javaArgv[(params->javaArgc)++] = &(arg[2]);
        else
         params->fuseArgv[(params->fuseArgc)++] = arg;
    }

    if (params->filesystemClassName == NULL)
    {
        printf("Missing option: -Cfuse.FuseFSClassName\n");
        return -1;
    }

    printf("%d fuse arguments:", params->fuseArgc);
    for (i = 0; i < params->fuseArgc; i++)
        printf(" %s", params->fuseArgv[i]);
    printf("\n");

    printf("%d java arguments:", params->javaArgc);
    for (i = 0; i < params->javaArgc; i++)
        printf(" %s", params->javaArgv[i]);
    printf("\n");

    printf("Java fuseFS: %s\n", params->filesystemClassName);


    fuse_main(params->fuseArgc, params->fuseArgv, &javafs_oper, params);

    return 0;
}
