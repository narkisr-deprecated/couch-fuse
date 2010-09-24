See [couch-fuse website](http://narkisr.github.com/couch-fuse/).

Known issues:

 * Nautilus write access to the FS is buggy, cli seems to be working fine.
       
Build: 
	# on ubuntu 10.04 64 and 32 bit 
	$ sudo aptitude install dh-make libfuse-dev sun-java6-jdk maven2 ruby rake couchdb curl
	$ git clone git://github.com/narkisr/fuse4j.git
	$ cd fuse4j/maven
	$ mvn clean install
	$ cd ..
	$ git clone git://github.com/narkisr/couch-fuse.git
	$ cd couch-fuse
	$ rake deb

Next:

 * FS hooks (post update hook in order to re-build couchdb views).
