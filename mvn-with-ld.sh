#!/bin/sh

CWD=`pwd`
FUSE_HOME=/usr
MOUNT_POINT=${CWD}/fake
JAVA_HOME=/usr/lib/jvm/java-6-sun

LD_LIBRARY_PATH=$FUSE_HOME/lib:${JAVA_HOME}/jre/lib/i386/server:${CWD}/../fuse4j/native
export LD_LIBRARY_PATH

#java -Djava.library.path=$LD_LIBRARY_PATH -Xdebug -Xrunjdwp:transport=dt_socket,address=8787,server=y,suspend=n fuse.FakeFilesystem $MOUNT_POINT -f
echo $LD_LIBRARY_PATH
mvn $@ -Djava.library.path=$LD_LIBRARY_PATH -o 
