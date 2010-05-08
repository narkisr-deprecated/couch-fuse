Couch-fuse is a Couchdb Fuse filesystem it enables a mount of a Couchdb databse into a local folder.
This comes handy for backup, editing and any other task that it easy to accomplish via a filesystem interface.

In order to install:

	$ wget http://github.com/downloads/narkisr/couch-fuse/couchfuse_0.1-1_i386.deb
	$ sudo dpkg -i couchfuse_0.1-1_i386.deb
	# if java and fuse-utils are not installed already
	$ sudo apt -f install

Usage:

	$ couchfuse -db db_name -path mount_path
	# each document is a file, rsync cat and other utilites work on them:
	$ cat mount_path/5195395990213004497
	$ rsync mount_path/ /some/backup/storage

Known limitations:

* At the moment the filesystem is read only (in the future write will be added).
* Binary attachments are not supported.
* This is an early release so expect issues.

Build: this project uses http://github.com/dtrott/fuse4j and maven, in order to build it get fuse4j working and hosted on you local repo:
	$ ./mvn-with-ld.sh clean install
