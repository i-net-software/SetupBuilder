#!/bin/sh
### BEGIN INIT INFO
# Provides:          {{name}}
# Required-Start:    $local_fs $network $remote_fs $syslog
# Required-Stop:     $local_fs $network $remote_fs $syslog
# Default-Start:     2 3 4 5
# Default-Stop:      0 1 6
# Short-Description: {{description}}
# Description:       {{description}}
### END INIT INFO

# PATH should only include /usr/* if it runs after the mountnfs.sh script
PATH=/sbin:/usr/sbin:/bin:/usr/bin
DESC="{{description}}"
NAME={{name}}
DAEMON=/usr/bin/java
DAEMON_ARGS="{{startArguments}}"
PIDFILE=/var/run/$NAME.pid
SCRIPTNAME=/etc/init.d/$NAME

# Exit if the package is not installed
[ -x "$DAEMON" ] || exit 0

# Read configuration variable file if it is present
[ -r /etc/default/$NAME ] && . /etc/default/$NAME

# Load the VERBOSE setting and other rcS variables
. /lib/init/vars.sh

# Define LSB log_* functions.
# Depend on lsb-base (>= 3.2-14) to ensure that this file is present
# and status_of_proc is working.
. /lib/lsb/init-functions

#
# Function that starts the daemon/service
#
do_start()
{
	# Return
	#   0 if daemon has been started
	#   1 if daemon was already running
	#   2 if daemon could not be started
	/usr/bin/jsvc -pidfile $PIDFILE -wait 10 -errfile "&2" $DAEMON_ARGS  || return 2
	return $?
}

#
# Function that stops the daemon/service
#
do_stop()
{
	# Return
	#   0 if daemon has been stopped
	#   1 if daemon was already stopped
	#   2 if daemon could not be stopped
	#   other if a failure occurred
	/usr/bin/jsvc -pidfile $PIDFILE -wait 10 -errfile "&2" -cp /usr/share/java/commons-daemon.jar -stop DAEMON_ARGS || return 1
	rm -f $PIDFILE
	return "$RETVAL"
}
 

case "$1" in
  start)
	log_daemon_msg "Starting" "$NAME"
	do_start
	case "$?" in
		0|1) log_end_msg 0 ;;
		2) log_end_msg 1 ;;
	esac
	;;
  stop)
	log_daemon_msg "Stopping" "$NAME"
	do_stop
	case "$?" in
		0|1) log_end_msg 0 ;;
		2) log_end_msg 1 ;;
	esac
	;;
  status)
	status_of_proc "$DAEMON" "$NAME" && exit 0 || exit $?
	;;
  restart|force-reload)
	#
	# If the "reload" option is implemented then remove the
	# 'force-reload' alias
	#
	log_daemon_msg "Restarting" "$NAME"
	do_stop
	case "$?" in
	  0|1)
		do_start
		case "$?" in
			0) log_end_msg 0 ;;
			1) log_end_msg 1 ;; # Old process is still running
			*) log_end_msg 1 ;; # Failed to start
		esac
		;;
	  *)
		# Failed to stop
		log_end_msg 1
		;;
	esac
	;;
  *)
	echo "Usage: $SCRIPTNAME {start|stop|status|restart|force-reload}" >&2
	exit 3
	;;
esac

: