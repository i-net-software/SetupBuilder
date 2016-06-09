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

DESC="{{displayName}}"
NAME={{name}}
MAINARCHIVE={{mainJar}}
PIDFILE=/var/run/$NAME.pid
WORKINGDIR='{{workdir}}'
EXEC=/usr/bin/java
PROC="$EXEC {{startArguments}}"



# Exit if the package is not installed
[ ! -f "$MAINARCHIVE" ] && exit 0


# Output colors
RED='\033[0;31m'
GREEN='\033[0;32m'
NC='\033[0m' # No Color

# Source config
if [ -f /etc/sysconfig/$NAME ] ; then
    . /etc/sysconfig/$NAME
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


# Function that starts the daemon/service
#
start() {
  # see if running
  local pids=$(pgrep -f "$PROC")

  if [ -n "$pids" ]; then
    echo "$NAME (pid $pids) is already running"
    return 0
  fi
  printf "%-50s%s" "Starting $NAME: " ''
  cd ${WORKINGDIR}
  $PROC &

  # save pid to file if you want
  echo $! > $PIDFILE

  # check again if running
  pgrep -f "$PROC" >/dev/null 2>&1
  eval_cmd $?
}

#
# Function that stops the daemon/service
#
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
