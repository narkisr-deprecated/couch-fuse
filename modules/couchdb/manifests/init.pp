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


  exec { 'apt-get update':
    command => '/usr/bin/apt-get update'
  }

  package { 'couchdb':
    ensure => present,
  }

  # see http://serverfault.com/a/181207
  service{ 'couchdb':
    ensure    => 'running',
    enable    => true,
    hasstatus => true,
    # it seems to fail at stop at times
    stop      => '/usr/bin/killall beam | true'
  }

  file { '/etc/couchdb/local.ini':
    source  => '/tmp/vagrant-puppet/modules-0/couchdb/files/local.ini',
    group   => 'couchdb',
    owner   => 'couchdb',
    require => Package['couchdb'],
    notify  => Service['couchdb']
  }
}
