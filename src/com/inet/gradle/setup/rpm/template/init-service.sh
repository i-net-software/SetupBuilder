#!/bin/sh
### BEGIN INIT INFO
# Provides:          {{name}}{{majorversion}}
# Required-Start:    $local_fs $network $remote_fs $syslog
# Required-Stop:     $local_fs $network $remote_fs $syslog
# Default-Start:     2 3 4 5
# Default-Stop:      0 1 6
# Short-Description: {{displayName}}
# Description:       {{description}}
### END INIT INFO

# PATH should only include /usr/* if it runs after the mountnfs.sh script
PATH=/sbin:/usr/sbin:/bin:/usr/bin
DESC="{{displayName}}"
NAME={{name}}
WAIT={{wait}}
DAEMON=/usr/bin/java
MAINARCHIVE={{mainJar}}
PIDFILE=/var/run/$NAME.pid
SCRIPTNAME=/etc/init.d/$NAME
WORKINGDIR='{{workdir}}'

# Exit if the package is not installed
[ ! -f "$MAINARCHIVE" ] && exit 0

# Read configuration variable file if it is present
[ -r /etc/default/$NAME ] && . /etc/default/$NAME

# Source function library
. /lib/lsb/init-functions

# Function that starts the daemon/service
#
do_start()
{
	# Return
	#   0 if daemon has been started
	#   1 if daemon was already running
	echo_passed "Starting $NAME: ";
	if [ -f "$PIDFILE" ]
	then
		PID='cat $PIDFILE'
		echo_passed $NAME already running: $PID
		return 1
	else
        if [ -z "$1" ]; then
            daemonize -c "$WORKINGDIR" -p $PIDFILE -l $PIDFILE -o "$WORKINGDIR/log.txt" -e "$WORKINGDIR/error.txt"  $DAEMON {{startArguments}}
        else
            # if a foreground daemon is summoned, just start the process with the arguments 
            cd "$WORKINGDIR" && $DAEMON {{startArguments}}
        fi
        
		return 0
	fi
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
	killproc -p $PIDFILE $NAME
	
	echo " Ausgabe $?"
	case "$?" in
		0) echo_success "$NAME was stopped"; rm -f $PIDFILE ;;
		1) echo_passed "(already running)";;
		7) echo_warning "$NAME was not running";;
		*) echo_warning "$NAME could not be stopped "
	esac
	
	return "$?"
}
 
case "$1" in
  start)
	echo "Starting" "$NAME"
	do_start
	case "$?" in
		0) exit 0 ;;
		1) echo_warning "(already running)"; exit 0 ;;
		2) exit 1 ;;
	esac
	;;
  daemon)
    log_daemon_msg "Starting as Daemon" "$NAME"
    do_start daemon
    case "$?" in
        0) log_end_msg 0 ;;
        1) log_success_msg "(already running)"; log_end_msg 0 ;;
    esac
    ;;
  stop)
	echo "Stopping" "$NAME"
	do_stop
	case "$?" in
		0|1) exit 0 ;;
		2) exit 1 ;;
	esac
	;;
  status)
	status -p $PIDFILE
	;;
  restart|force-reload)
	#
	# If the "reload" option is implemented then remove the
	# 'force-reload' alias
	#
	do_stop
	case "$?" in
	  0|1)
		do_start
		case "$?" in
			0) exit 0 ;;
			1) echo_warning "(already running)"; exit 0 ;;
			2) exit 1 ;;
		esac
		;;
	  *)
		# Failed to stop
		exit 1
		;;
	esac
	;;
  *)
	echo "Usage: $SCRIPTNAME {start|stop|status|restart|force-reload|daemon}" >&2
	exit 3
	;;
esac

: