#!/bin/bash
#
# Description: {{description}}
#
# chkconfig: - 80 20
#

# Source function library.
[ -r "/etc/init.d/functions" ] && . /etc/init.d/functions

NAME={{name}}
DAEMON_USER={{daemonUser}}
PIDFILE=/var/run/$NAME.pid
DAEMON=/usr/bin/java
WORKINGDIR="{{workdir}}"
MAINARCHIVE="{{mainJar}}"
MAINCLASS={{mainClass}}
STARTARGUMENTS="{{startArguments}}"

# Output colors
RED='\033[0;31m'
GREEN='\033[0;32m'
NC='\033[0m' # No Color

# Source config
[ -r "/etc/sysconfig/$NAME" ] && . "/etc/sysconfig/$NAME"

LINESTATE=""
prep_cmd() {
    LINESTATE="$1"
    printf "\r%s" $LINESTATE 
}

eval_cmd() {
    local rc=$1

    if [ $rc -eq 0 ]; then
        STATE="[ ${GREEN}OK${NC} ]"
    else
        STATE="[ ${RED}FAILED${NC} ]"
    fi

    printf "\r%*s\r%s\n" $(tput cols) "$(printf "$STATE")" "$LINESTATE"
    LINESTATE=""
    return $rc
}

# check if PID from PIDFILE is a process
check_pid_exists() {
    [ -f "$PIDFILE" ] && [ -n "$(ps hp $(head -n1 "$PIDFILE"))" ] 
}

start() {
    # see if running
    check_pid_exists && echo "$NAME (pid $(cat "$PIDFILE")) is already running" && return 0 || :

    BACKGROUND="&&"
    if [ -z "$1" ]; then
        BACKGROUND="&"
    fi

    prep_cmd "Starting $NAME:"
    cd "${WORKINGDIR}" && su ${DAEMON_USER} -c "$DAEMON -cp \"${MAINARCHIVE}\" ${MAINCLASS} ${STARTARGUMENTS} > \"/tmp/$NAME.out\" $BACKGROUND echo \$! > \"/tmp/$NAME.pid\""

    # save pid to file if you want
    ([ -f "/tmp/$NAME.pid" ] && cat "/tmp/$NAME.pid" > "$PIDFILE" && rm "/tmp/$NAME.pid" ) || echo "PID-file ($PIDFILE) could not be created"

    # check again if running
    sleep 5
    check_pid_exists
    eval_cmd $?
}

stop() {
    # see if running
    check_pid_exists || echo "$NAME is not running" && return 0

    prep_cmd "Stopping $NAME:"
    kill -9 `pgrep -F "$PIDFILE"`
    eval_cmd $?
    rm -f $PIDFILE
}

status() {
    # see if running
    check_pid_exists && echo "$NAME is running with pid $(cat "$PIDFILE")" || echo "$NAME is not running"
}

{{additionalServiceScript}}

case $1 in
    start)
        start
        ;;
    daemon)
        start daemon
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
