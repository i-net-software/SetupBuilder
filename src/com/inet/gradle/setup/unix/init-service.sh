#!/bin/bash
#
# chkconfig: 2345 80 20
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
APPNAME={{name}}
WAIT={{wait}}
DAEMON_USER={{daemonUser}}
DAEMON_EXEC=/usr/bin/java
MAINARCHIVE="{{mainJar}}"
MAINCLASS="{{mainClass}}"
PIDFILE=/var/run/$APPNAME.pid
SCRIPTNAME=${0##*/}
WORKINGDIR="{{workdir}}"
STARTARGUMENTS="{{startArguments}}"

if [ -e /lib/lsb/init-functions ]; then
    . /lib/lsb/init-functions
elif [ -e /etc/init.d/functions ] ; then
    . /etc/init.d/functions
fi

checkfunc() {
    # echo "Checking $1"
    [ type "$1" 2> /dev/null ] && return 0 || return 1
}

if ! checkfunc "log_daemon_msg" ; then
# no lsb setup?  no problem.  we will add them in

log_use_fancy_output () {
    TPUT=/usr/bin/tput
    EXPR=/usr/bin/expr
    if [ -t 1 ] && [ "x$TERM" != "" ] && [ "x$TERM" != "xdumb" ] && [ -x $TPUT ] && [ -x $EXPR ] && $TPUT hpa 60 >/dev/null 2>&1 && $TPUT setaf 1 >/dev/null 2>&1; then
        [ -z $FANCYTTY ] && FANCYTTY=1 || true
    else
        FANCYTTY=0
    fi
    case "$FANCYTTY" in
        1|Y|yes|true)   true;;
        *)              false;;
    esac
}

log_success_msg () {
    if [ -n "${1:-}" ]; then
        log_begin_msg $@
    fi
    log_end_msg 0
}

log_failure_msg () {
    if [ -n "${1:-}" ]; then
        log_begin_msg $@
    fi
    log_end_msg 1 || true
}

log_warning_msg () {
    if [ -n "${1:-}" ]; then
        log_begin_msg $@
    fi
    log_end_msg 255 || true
}

#
# NON-LSB HELPER FUNCTIONS
#
# int get_lsb_header_val (char *scriptpathname, char *key)
get_lsb_header_val () {
        if [ ! -f "$1" ] || [ -z "${2:-}" ]; then
                return 1
        fi
        LSB_S="### BEGIN INIT INFO"
        LSB_E="### END INIT INFO"
        sed -n "/$LSB_S/,/$LSB_E/ s/# $2: \(.*\)/\1/p" $1
}

# int log_begin_message (char *message)
log_begin_msg () {
    if [ -z "${1:-}" ]; then
        return 1
    fi
    echo -n "$@"
}

# Sample usage:
# log_daemon_msg "Starting GNOME Login Manager" "gdm"
#
# On Debian, would output "Starting GNOME Login Manager: gdm"
# On Ubuntu, would output " * Starting GNOME Login Manager..."
#
# If the second argument is omitted, logging suitable for use with
# log_progress_msg() is used:
#
# log_daemon_msg "Starting remote filesystem services"
#
# On Debian, would output "Starting remote filesystem services:"
# On Ubuntu, would output " * Starting remote filesystem services..."

log_daemon_msg () {
    if [ -z "${1:-}" ]; then
        return 1
    fi
    log_daemon_msg_pre "$@"

    if [ -z "${2:-}" ]; then
        echo -n "$1:"
        return
    fi

    echo -n "$1: $2"
    log_daemon_msg_post "$@"
}

# #319739
#
# Per policy docs:
#
#     log_daemon_msg "Starting remote file system services"
#     log_progress_msg "nfsd"; start-stop-daemon --start --quiet nfsd
#     log_progress_msg "mountd"; start-stop-daemon --start --quiet mountd
#     log_progress_msg "ugidd"; start-stop-daemon --start --quiet ugidd
#     log_end_msg 0
#
# You could also do something fancy with log_end_msg here based on the
# return values of start-stop-daemon; this is left as an exercise for
# the reader...
#
# On Ubuntu, one would expect log_progress_msg to be a no-op.
log_progress_msg () {
    if [ -z "${1:-}" ]; then
        return 1
    fi
    echo -n " $@"
}


# int log_end_message (int exitstatus)
log_end_msg () {
    # If no arguments were passed, return
    if [ -z "${1:-}" ]; then
        return 1
    fi

    retval=$1

    log_end_msg_pre "$@"

    # Only do the fancy stuff if we have an appropriate terminal
    # and if /usr is already mounted
    if log_use_fancy_output; then
        RED=`$TPUT setaf 1`
        YELLOW=`$TPUT setaf 3`
        NORMAL=`$TPUT op`
    else
        RED=''
        YELLOW=''
        NORMAL=''
    fi

    if [ $1 -eq 0 ]; then
        echo "."
    elif [ $1 -eq 255 ]; then
        /bin/echo -e " ${YELLOW}(warning).${NORMAL}"
    else
        /bin/echo -e " ${RED}failed!${NORMAL}"
    fi
    log_end_msg_post "$@"
    return $retval
}

log_action_msg () {
    echo "$@."
}

log_action_begin_msg () {
    echo -n "$@..."
}

log_action_cont_msg () {
    echo -n "$@..."
}

log_action_end_msg () {
    log_action_end_msg_pre "$@"
    if [ -z "${2:-}" ]; then
        end="."
    else
        end=" ($2)."
    fi

    if [ $1 -eq 0 ]; then
        echo "done${end}"
    else
        if log_use_fancy_output; then
            RED=`$TPUT setaf 1`
            NORMAL=`$TPUT op`
            /bin/echo -e "${RED}failed${end}${NORMAL}"
        else
            echo "failed${end}"
        fi
    fi
    log_action_end_msg_post "$@"
}

# Hooks for /etc/lsb-base-logging.sh
log_daemon_msg_pre () { :; }
log_daemon_msg_post () { :; }
log_end_msg_pre () { :; }
log_end_msg_post () { :; }
log_action_end_msg_pre () { :; }
log_action_end_msg_post () { :; }

fi

# Read configuration variable file if it is present
[ -r "/etc/default/$APPNAME" ] && . "/etc/default/$APPNAME" || :
[ -r "/etc/sysconfig/$APPNAME" ] && . "/etc/sysconfig/$APPNAME" || :

[ "$(id $DAEMON_USER 2> /dev/null; echo $?)" == "0" ] && log_failure_msg "'$DAEMON_USER' is not a user. Please create a user account first." && exit 1 || :

# check for daemon program to be present
[ ! -x "$DAEMON_EXEC" ] && log_failure_msg "The program '$DAEMON_EXEC' does not exist" && exit 1 || :

# Exit if the package is not installed
[ ! -f "$MAINARCHIVE" ] && log_failure_msg "File '$MAINARCHIVE' not found" && exit 1 || :

# Load the VERBOSE setting and other rcS variables
[ -r "/lib/init/vars.sh" ] && . "/lib/init/vars.sh" || :

checkstatus() {
    if checkfunc "start-stop-daemon"; then
        echo " (ssd) "
        start-stop-daemon  --chuid "$DAEMON_USER" --test --start --chdir "$WORKINGDIR" --pidfile "$PIDFILE" --exec "$DAEMON_EXEC" >/dev/null && return 0 || return 1
    else
        [ -f "$PIDFILE" ] && [ ! -z "$(head -n1 $PIDFILE)" ] && [ ! -z "$(ps hp $(head -n1 $PIDFILE) 2> /dev/null)" ] && return 0 || return 1
    fi
}

start() {
    checkstatus && log_warning_msg "$APPNAME already running, can't start it" && log_end_msg 0 && return 1 || :

    # this should be a background process by default.
    BACKGROUND="& echo \$! > \"/tmp/${APPNAME}.pid\""
    if [ ! -z "$1" ]; then
        # if a parameter is set, this is NOT a background daemon 
        BACKGROUND=""
    fi

    log_daemon_msg "Starting" "$APPNAME"
    if checkfunc "start-stop-daemon"; then
        if [ ! -z "${BACKGROUND}" ]; then
            BACKGROUND="-b"
        fi
        start-stop-daemon  --chuid "$DAEMON_USER" ${BACKGROUND} --chdir "$WORKINGDIR" --make-pidfile --start --pidfile "$PIDFILE" --exec $DAEMON_EXEC -- ${STARTARGUMENTS} -cp "${MAINARCHIVE}" ${MAINCLASS}
    elif checkfunc "start_daemon"; then
        start_daemon -u "$DAEMON_USER" /bin/bash -c "cd \"${WORKINGDIR}\" ; $DAEMON_EXEC ${STARTARGUMENTS} -cp \"${MAINARCHIVE}\" ${MAINCLASS} > \"/tmp/${APPNAME}-${DAEMON_USER}.out\" ${BACKGROUND}"
    elif checkfunc "daemon"; then
        daemon -u "$DAEMON_USER" "cd \"${WORKINGDIR}\" ; $DAEMON_EXEC ${STARTARGUMENTS} -cp \"${MAINARCHIVE}\" ${MAINCLASS} > \"/tmp/${APPNAME}-${DAEMON_USER}.out\" ${BACKGROUND}"
    else
        su - --shell=/bin/bash $DAEMON_USER -c "cd \"${WORKINGDIR}\" ; $DAEMON_EXEC ${STARTARGUMENTS} -cp \"${MAINARCHIVE}\" ${MAINCLASS} > \"/tmp/${APPNAME}-${DAEMON_USER}.out\" ${BACKGROUND}"
    fi

    # return if this was not a background process
    if [ -z "${BACKGROUND}" ]; then
        log_end_msg 0
        return 0
    fi

    # only wait if it did start OK
    [ -f "/tmp/$APPNAME.pid" ] && mv "/tmp/$APPNAME.pid" "$PIDFILE" && checkstatus && sleep $WAIT || :

    # save pid to file if you want
    checkstatus || log_failure_msg "PID-file ($PIDFILE) could not be created"

    RETVAL=$?
    [ $RETVAL -ne 0 ] && rm -f "$PIDFILE"
    log_end_msg $RETVAL

    return $RETVAL
}

stop() {
    log_daemon_msg "Stopping" "$APPNAME"

    ! checkstatus && log_progress_msg "$APPNAME not running, no need to stop it" && log_end_msg 0 && return 0 || :
    RETVAL=0
    
    if checkfunc "start-stop-daemon"; then
        start-stop-daemon  --chuid "$DAEMON_USER" --stop --quiet --retry=TERM/30/KILL/5 --pidfile "$PIDFILE"
    elif checkfunc "killproc"; then
        killproc -p "$PIDFILE" "$APPNAME"
    else
        kill -9 $(head -n1 "$PIDFILE") 2> /dev/null
        rm -f "$PIDFILE"
        log_end_msg 0 && return 0
    fi

    RETVAL=$?
    [ $RETVAL -eq 0 ] && rm -f "$PIDFILE"
    log_end_msg $RETVAL

    return $RETVAL
}

{{additionalServiceScript}}

case "$1" in
    start)
        start
        ;;
     stop)
        stop
        ;;
    daemon)
        start daemon
        ;;
     restart)
        stop
        sleep 3
        start
        ;;
     status)
        checkstatus && log_success_msg "$APPNAME is running with pid $(cat "$PIDFILE")" || log_success_msg $"$APPNAME is not running"
        ;;
     *)
        echo "Usage: $0 {start|stop|restart|status}"
        exit 1
esac
exit $?

