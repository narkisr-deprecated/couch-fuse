#!/bin/sh
java -Djava.library.path=/usr/lib:/usr/lib/jvm/java-6-sun/jre/lib/i386/server:/usr/share/couchfuse/native -jar target/couch-fuse-0.1-jar-with-dependencies.jar $@ -run-valid true

exitValue=$? 

if [ $exitValue != 0 ] 
then 
	exit $exitValue 
fi 

java -Djava.library.path=/usr/lib:/usr/lib/jvm/java-6-sun/jre/lib/i386/server:/usr/share/couchfuse/native -jar target/couch-fuse-0.1-jar-with-dependencies.jar $@  > /var/log/couchfuse.log &
