#!/bin/sh

ln -s /usr/share/couchfuse/couchfuse /usr/bin/
chmod +x /usr/bin/couchfuse
touch /var/log/couchfuse.log
chmod 666 /var/log/couchfuse.log
