Couch-fuse is a Couchdb Fuse filesystem it enables a mount of a Couchdb databse into a local folder.
This comes handy for backup, editing and any other task that it easy to accomplish via a filesystem interface.

In order to install:

	$ sudo dpkg -i couchfuse_0.1-1_i386.deb
	# if there are missing dependencies (java and fuse-utils)
	$ sudo apt -f install

Usage:
	$ couchfuse -db db_name -path mount_path

Known limitations:

* At the moment the filesystem is read only (in the future write will be added).
* Binary attachments are not supported.

