# Perquisites

Couchfuse creates both a jar file and a deb package, note that currently only Ubuntu 11.10 64bit is supported.

In order to build it the following perquisite should be meet:

## Installations

* Virtualbox ([Vagrant](http://vagrantup.com/) is used to hold a Couchdb instance)

* [lein](https://github.com/technomancy/leiningen)

* [RVM](http://beginrescueend.com/rvm/install/) and Ruby 1.9.2 (see .rvmrc for exact version), create couch-fuse gemset

* [sun-jdk-1.6](http://superuser.com/questions/353983/how-do-i-install-the-sun-java-sdk-in-ubuntu-11-10-oneric)


## Packages and sandbox

fuse4j:


```
 $ sudo aptitude install maven
 $ git clone git://github.com/narkisr/fuse4j.git
 $ cd fuse4j/maven/fuse4j-core
 $ mvn install 
 $ cd fuse4j/maven/
 # installing only parent pom
 $ mvn install -N 
```

Bundler and gems:

```bash
 $ cd couch-fuse
 $ gem install bundle    
 $ bundle install 
```

Install Ubuntu 12.04 box and fire the couchdb sandbox machine:

```bash
  $ vagrant box add ubuntu-12.04 http://files.vagrantup.com/precise64.box
  # This will start a VM with a couchdb instance port forwarded to port 5983
  $ vagrant up
```

## Build

Run tests:

```bash 
  $ lein deps
  $ lein native-deps
  # Running lein through rake in order for it to set LD_LIBRARY_PATH
  $ rake 'lein[compile]'
  $ rake 'lein[test]'
```

Build deb:

```bash 
 $ rake deb
```

