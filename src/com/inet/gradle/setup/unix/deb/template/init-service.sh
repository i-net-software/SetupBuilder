#!/bin/bash
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
DAEMON_USER={{daemonUser}}
DAEMON=/usr/bin/java
MAINARCHIVE="{{mainJar}}"
MAINCLASS="{{mainClass}}"
PIDFILE=/var/run/$NAME.pid
SCRIPTNAME=/etc/init.d/$NAME
WORKINGDIR="{{workdir}}"
STARTARGUMENTS="{{startArguments}}"

# Exit if the package is not installed
[ ! -f "$MAINARCHIVE" ] && echo "File '$MAINARCHIVE' not found" && exit 1

# Read configuration variable file if it is present
[ -r "/etc/default/$NAME" ] && . /etc/default/$NAME

# Load the VERBOSE setting and other rcS variables
[ -r "/lib/init/vars.sh" ] && . /lib/init/vars.sh

# Define LSB log_* functions.
# Depend on lsb-base (>= 3.2-14) to ensure that this file is present
# and status_of_proc is working.
[ -r "/lib/lsb/init-functions"] && . /lib/lsb/init-functions

# Make sure, that the locale is set. Specifically if started after boot
[ -r /etc/default/locale ] && . /etc/default/locale && export LANG

# Function that starts the daemon/service
#
do_start()
{
	# Return
	#   0 if daemon has been started
	#   1 if daemon was already running
	#   2 if daemon could not be started
	start-stop-daemon --chuid $DAEMON_USER --start --chdir "$WORKINGDIR" --pidfile $PIDFILE --exec $DAEMON --test > /dev/null \
		|| return 1

    BACKGROUND=""
    if [ -z "$1" ]; then
        BACKGROUND="-b"
    fi
    
    start-stop-daemon  --chuid $DAEMON_USER $BACKGROUND --chdir "$WORKINGDIR" --make-pidfile --start --pidfile $PIDFILE --exec $DAEMON -- \
        -cp "${MAINARCHIVE}" ${MAINCLASS} ${STARTARGUMENTS} \
        || return 2
	
	if [ ! -z "$1" ]; then
        sleep $WAIT
        if start-stop-daemon  --chuid $DAEMON_USER --test --start --chdir "$WORKINGDIR" --pidfile "$PIDFILE" --exec $DAEMON >/dev/null; then
            if [ -f "$PIDFILE" ]; then
                rm -f "$PIDFILE"
            fi
            return 1
        else
            return 0
        fi
    else
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
	start-stop-daemon  --chuid $DAEMON_USER --stop --quiet --retry=TERM/30/KILL/5 --pidfile $PIDFILE 
	RETVAL="$?"
	[ "$RETVAL" = 2 ] && return 2
	rm -f $PIDFILE
	return "$RETVAL"
}
 
case "$1" in
  start)
    log_daemon_msg "Starting" "$NAME"
    do_start
    case "$?" in
        0) log_end_msg 0 ;;
        1) log_success_msg "(already running)"; log_end_msg 0 ;;
        2) log_end_msg 1 ;;
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
	log_daemon_msg "Stopping" "$NAME"
	do_stop
	case "$?" in
		0|1) log_end_msg 0 ;;
		2) log_end_msg 1 ;;
	esac
	;;
  status)
	if start-stop-daemon  --chuid $DAEMON_USER --test --start --chdir "$WORKINGDIR" --pidfile "$PIDFILE" --exec $DAEMON >/dev/null; then
	    log_success_msg "$NAME is not running."
	else
	    log_success_msg "$NAME is running."
	fi
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
	echo "Usage: $SCRIPTNAME {start|stop|status|restart|force-reload|daemon}" >&2
	exit 3
	;;
esac

:
