Couch-fuse its a Couchdb Fuse filesystem it enables a mount of a Couchdb database into a local folder.
This comes handy for backup, editing and any other task that it easy to accomplish via a filesystem interface, see this [demo](http://www.youtube.com/watch?v=PxD9SroHhWE).

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
	mount_path/.foo/foo.json
	mount_path/foo
	# we can backup the db using rsync
	$ rsync -auv mount_path/ /some/backup/storage
	# document is editable, json must be kept valid
	$ vi mount_path/.foo/foo.json
	# attachmens show up under the attachment folder
	$ cp another.jpeg mount_path/foo && ls mount_path/foo
	another.jpeg
	# in order to delete a document both attahments and meta folder should be cleared, starting with attachments
	$ rm -r mount_path/.foo
	rm: cannot remove directory `fake/.foo': Operation not permitted
	# this will work
	$ rm -r mount_path/foo && rm -r mount_path/.foo

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
