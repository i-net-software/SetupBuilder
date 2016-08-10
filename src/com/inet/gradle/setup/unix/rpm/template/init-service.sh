#!/bin/bash
#
# Description:       {{description}}
#
# chkconfig: - 80 20
#

# Source function library.
. /etc/init.d/functions

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

if [ ! -z "$STARTARGUMENTS" ]; then
    PROC="$DAEMON -cp ${MAINARCHIVE} ${MAINCLASS} ${STARTARGUMENTS}"
else
    PROC="$DAEMON -cp ${MAINARCHIVE} ${MAINCLASS}"
fi

eval_cmd() {
  local rc=$1
  if [ $rc -eq 0 ]; then
    printf "[  ${GREEN}OK${NC}  ]\n"
  else
    printf "[${RED}FAILED${NC}]\n"
  fi
  return $rc
}

start() {
  # see if running
  local pids=$(pgrep -f "$PROC")

  if [ -n "$pids" ]; then
    echo "$NAME (pid $pids) is already running"
    return 0
  fi
  printf "%-50s%s" "Starting $NAME: " ''
  cd "${WORKINGDIR}"
  if [[ "${DAEMON_USER}" = "root" ]]; then
    $PROC &
  else
    su ${DAEMON_USER} -c "$PROC &"
  fi

  # save pid to file if you want
  echo $! > $PIDFILE

  # check again if running
  sleep 5
  pgrep -f "$PROC" >/dev/null 2>&1
  eval_cmd $?
}

stop() {
  # see if running
  local pids=$(pgrep -f "$PROC")

  if [ -z "$pids" ]; then
    echo "$NAME not running"
    return 0
  fi
  printf "%-50s%s" "Stopping $NAME: " ''
  rm -f $PIDFILE
  kill -9 $pids
  eval_cmd $?
}

status() {
  # see if running
  local pids=$(pgrep -f "$PROC")

  if [ -n "$pids" ]; then
    echo "$NAME (pid $pids) is running"
  else
    echo "$NAME is stopped"
  fi
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
