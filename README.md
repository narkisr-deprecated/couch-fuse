Couch-fuse is a Couchdb Fuse filesystem it enables a mount of a Couchdb databse into a local folder.
This comes handy for backup, editing and any other task that it easy to accomplish via a filesystem interface.

In order to install:

	$ wget http://github.com/downloads/narkisr/couch-fuse/couchfuse_0.1-1_i386.deb
	$ sudo dpkg -i couchfuse_0.1-1_i386.deb
	# if java and fuse-utils are not installed already
	$ sudo apt -f install

Usage:

	$ couchfuse -db db_name -path mount_path
	# each document is represented by folder, rsync cat and other utilites work on them:
	$ cat mount_path/5195395990213004497
	$ rsync mount_path/ /some/backup/storage
        # creating new documents is mkdir away
        $ 

Known limitations:

* At the moment the filesystem is read only (in the future write will be added).
* Binary attachments are not supported.
* This is an early release so expect issues.

Build: 
        $ sudo aptitude install dh-make libfuse-dev sun-java6-jdk maven2 ruby rake couchdb
        $ git clone git://github.com/narkisr/fuse4j.git
        $ cd fuse4j/maven
        $ maven clean install
        $ cd ..
        $ git clone git://github.com/narkisr/couch-fuse.git
        $ cd couch-fuse
        $ curl -X PUT http://localhost:5984/playground # a db used in testing
        $ rake deb
