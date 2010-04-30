#
# Regular cron jobs for the couchfuse package
#
0 4	* * *	root	[ -x /usr/bin/couchfuse_maintenance ] && /usr/bin/couchfuse_maintenance
