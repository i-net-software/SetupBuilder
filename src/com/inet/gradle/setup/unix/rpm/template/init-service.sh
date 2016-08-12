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
[ -r /etc/sysconfig/$NAME ] && . /etc/sysconfig/$NAME

eval_cmd() {
    local rc=$1
    if [ $rc -eq 0 ]; then
        printf "[  ${GREEN}OK${NC}  ]\n"
    else
        printf "[  ${RED}FAILED${NC}  ]\n"
    fi
    return $rc
}

start() {
    # see if running
    ([ -f "$PIDFILE" ] && [ `pgrep -F "$PIDFILE"` ] && echo "$NAME (pid $(cat "$PIDFILE")) is already running" && return 0) || :

    printf "%-50s%s" "Starting $NAME: " ''
    cd "${WORKINGDIR}" && su ${DAEMON_USER} -c "$DAEMON -cp \"${MAINARCHIVE}\" ${MAINCLASS} ${STARTARGUMENTS} &"

    # save pid to file if you want
    echo $! > $PIDFILE

    # check again if running
    sleep 5
    pgrep -F "$PIDFILE"
    eval_cmd $?
}

stop() {
    # see if running
    (([ ! -f "$PIDFILE" ] || [ -z `pgrep -F "$PIDFILE"` ]) && echo "$NAME is not running" && return 0) || :

    printf "%-50s%s" "Stopping $NAME: " ''
    kill -9 `pgrep -F "$PIDFILE"`
    eval_cmd $?
    rm -f $PIDFILE
}

status() {
    # see if running
    (([ ! -f "$PIDFILE" ] || [ -z `pgrep -F "$PIDFILE"` ]) && echo "$NAME is not running" && return 0) || :
    ([ -f "$PIDFILE" ] && [ `pgrep -F "$PIDFILE"` ] && echo "$NAME is running with pid $(cat "$PIDFILE")" && return 0) || :
}

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
    restart)
        stop
        sleep 1
        start
        ;;
    *)
        echo "Usage: $0 {start|stop|status|restart}"
        exit 1
esac

exit $?
