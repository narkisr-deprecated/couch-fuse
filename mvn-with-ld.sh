#!/bin/sh

# This script launches the JVM directly which loads the fuse native library.
# It is not possible to fork a running JVM into the background, hence this script can only be used in the foreground.
# Which is probably the best option for debugging.
# If you want to fork into the background use the native launcher.

PROJECT_NAME=fuse4j
CWD=`pwd`

FUSE_HOME=/usr
MOUNT_POINT=${CWD}/fake
FS_CLASS=fuse/FakeFilesystem
VERSION=2.4.0.0-SNAPSHOT
JAVA_HOME=/usr/lib/jvm/java-6-sun


M2_REPO=${HOME}/.m2/repository

LD_LIBRARY_PATH=$FUSE_HOME/lib:${JAVA_HOME}/jre/lib/i386/server:${CWD}/../fuse4j/native
export LD_LIBRARY_PATH

CLASSPATH=""
CLASSPATH="$CLASSPATH:$M2_REPO/${PROJECT_NAME}/${PROJECT_NAME}-core/${VERSION}/${PROJECT_NAME}-core-${VERSION}.jar"
CLASSPATH="$CLASSPATH:$M2_REPO/${PROJECT_NAME}/${PROJECT_NAME}-fakefs/${VERSION}/${PROJECT_NAME}-fakefs-${VERSION}.jar"
CLASSPATH="$CLASSPATH:$M2_REPO/commons-logging/commons-logging/1.1.1/commons-logging-1.1.1.jar:$M2_REPO/log4j/log4j/1.2.13/log4j-1.2.13.jar:${CWD}"
export CLASSPATH


#java -Djava.library.path=$LD_LIBRARY_PATH -Xdebug -Xrunjdwp:transport=dt_socket,address=8787,server=y,suspend=n fuse.FakeFilesystem $MOUNT_POINT -f
mvn clean install -Djava.library.path=$LD_LIBRARY_PATH
