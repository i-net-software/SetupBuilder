#!/bin/bash
#
# Description: {{description}}
#
# chkconfig: - 80 20
# pidfile: /var/run/{{name}}.pid
#

# Source function library.
[ ! -r "/etc/init.d/functions" ] && echo "/etc/init.d/functions is mising" && exit 1 || . /etc/init.d/functions

NAME={{name}}
DAEMON_USER={{daemonUser}}
PIDFILE=/var/run/$NAME.pid
DAEMON=/usr/bin/java
WORKINGDIR="{{workdir}}"
MAINARCHIVE="{{mainJar}}"
MAINCLASS={{mainClass}}
STARTARGUMENTS="{{startArguments}}"

# check for daemon program to be present
[ ! -x "$DAEMON" ] && echo "The program '$DAEMON' does not exist" && exit 1 || :

# Source config
[ -r "/etc/sysconfig/$NAME" ] && . "/etc/sysconfig/$NAME"

RETVAL=0

start() {
    daemon --check "$NAME" --user "$DAEMON_USER" --pidfile "$PIDFILE" "cd \"${WORKINGDIR}\" ; $DAEMON -cp \"${MAINARCHIVE}\" ${MAINCLASS} ${STARTARGUMENTS} > \"/tmp/$NAME.out\" & echo \$! > \"/tmp/$NAME.pid\""
    RETVAL=$?
    
    # save pid to file if you want
    ([ -f "/tmp/$NAME.pid" ] && cat "/tmp/$NAME.pid" > "$PIDFILE" && rm "/tmp/$NAME.pid" ) || echo "PID-file ($PIDFILE) could not be created"

    echo
    return "$RETVAL"
}

stop() {
    killproc -p "$PIDFILE" "$NAME"
    RETVAL=$?
    echo

    [ $RETVAL = 0 ] && rm -f "$PIDFILE"
    return "$RETVAL"
}

status() {
    # see if running
    [ -f "$PIDFILE" ] && [ -n "$(ps hp $(head -n1 "$PIDFILE"))" ] &&  echo "$NAME is running with pid $(cat "$PIDFILE")" || echo $"$NAME is not running"
}

{{additionalServiceScript}}

case $1 in
    start)
        start
        ;;
    stop)
        stop
        ;;
    status)
        status
        ;;
    restart|force-reload)
        stop
        sleep 1
        start
        ;;
    *)
        echo "Usage: $0 {start|stop|status|restart}"
        exit 1
esac

exit $?
