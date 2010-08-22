Couch-fuse its a Couchdb Fuse filesystem it enables a mount of a Couchdb databse into a local folder.
This comes handy for backup, editing and any other task that it easy to accomplish via a filesystem interface, see this [demo](http://www.youtube.com/watch?v=ps3-CnqKVxU).

In order to install:

	$ wget http://github.com/downloads/narkisr/couch-fuse/couch-fuse_0.3-1_amd64.deb
	$ sudo dpkg -i couch-fuse_0.3-1_amd64.deb
	# if java and fuse-utils are not installed already
	$ sudo apt -f install

Usage:

	$ couchfuse -db db_name -path mount_path
	# each document is represented by folder, rsync cat and other utilites work:
	$ cat mount_path/5195395990213004497/5195395990213004497.json
	$ rsync mount_path/ /some/backup/storage
	# creating new documents is mkdir away, dir name is document id
	$ mkdir foo
	# documents are editable, you must keep the json valid or update will fail
	$ vi mount_path/foo/foo.json
	# exising attachmens show up under the document folder
	$ ls mount_path/foo/
	foo.json  80x15.png  another.jpeg
	# create an attachment
	$ touch mount_path/foo/bla.txt
	# edit its content
	$ vim mount_path/foo/bla.txt

Known issues:

 * Accessing attachment via nautilus zero out file content (this issue will be resolved on the next version).
       
Build: 
	# on ubuntu 10.04 64 and 32 bit 
	$ sudo aptitude install dh-make libfuse-dev sun-java6-jdk maven2 ruby rake couchdb curl
	$ git clone git://github.com/narkisr/fuse4j.git
	$ cd fuse4j/maven
	$ maven clean install
	$ cd ..
	$ git clone git://github.com/narkisr/couch-fuse.git
	$ cd couch-fuse
	$ rake deb

Next:

 * FS hooks (post update hook in order to re-build couchdb views).
