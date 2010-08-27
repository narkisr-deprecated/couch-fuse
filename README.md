Couch-fuse its a Couchdb Fuse filesystem it enables a mount of a Couchdb database into a local folder.
This comes handy for backup, editing and any other task that it easy to accomplish via a filesystem interface, see this [demo](http://www.youtube.com/watch?v=ps3-CnqKVxU).

In order to install:

	$ wget http://github.com/downloads/narkisr/couch-fuse/couch-fuse_0.3-1_amd64.deb
	$ sudo dpkg -i couch-fuse_0.3-1_amd64.deb
	# if java and fuse-utils are not installed already
	$ sudo apt -f install

Usage:

	$ couchfuse -db db_name -path mount_path
	# in order to create a new doc
	$ mkdir mount_path/foo
	# each document is represented by two folders:
	  * the first is the meta folder that contains the document json
	  * the second contains attachments
	$ find mount_path/
	mount_path/.foo
	mount_path/foo
	# we can backup the db using rsync
	$ rsync mount_path/ /some/backup/storage
	# documents are editable, you must keep the json valid or update will fail
	$ vi mount_path/.foo/foo.json
	# exising attachmens show up under the attachment folder
	$ ls mount_path/bar/
	foo.json  80x15.png  another.jpeg
	# adding attchments is simple
	$ vim mount_path/foo/bla.txt
	# in order to delete a document both attahments and meta folder should be cleared, starting with the attahments
	$ rm -r mount_path/.bar
	rm: cannot remove directory `fake/.bla': Operation not permitted
	$ rm -r mount_path/bar && rm -r mount_path/.bar

Known issues:

 * Nautilus write access to the FS is buggy, cli seems to be working find.
       
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
