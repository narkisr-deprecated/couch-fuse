# Class: couchdb
#
# This module manages couchdb
#
# Parameters:
#
# Actions:
#
# Requires:
#
# Sample Usage:
#
# [Remember: No empty lines between comments and class definition]
class couchdb {

  group{ 'puppet':
    ensure  => present
  }

  file { '/etc/couchdb/local.ini':
    source => '/tmp/vagrant-puppet/modules-0/couchdb/files/local.ini',
    group  => 'couchdb',
    owner  => 'couchdb'
  }

  exec { 'apt-get update':
    command => '/usr/bin/apt-get update'
  }

  package { 'couchdb':
    ensure => present,
  }

}
