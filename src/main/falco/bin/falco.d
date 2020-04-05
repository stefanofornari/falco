#!/bin/bash
# /etc/init.d/serverone

### BEGIN INIT INFO
# Provides:          serverone
# Required-Start:    $remote_fs $syslog
# Required-Stop:     $remote_fs $syslog
# Default-Start:     2 3 4 5
# Default-Stop:      0 1 6
# Short-Description: ServerOne service
# Description:       ServerOne service
### END INIT INFO

#
# REMEMBER:
# 1. set USER and ROOT
# 2. run sudo update-rc.d serverone.d defaults
#
USER="pi"
ROOT="/home/pi/serverone"

case "$1" in 
    start)
        echo "Starting serverone"
        su - $USER -c "$ROOT/bin/run &"
        ;;
    stop)
        echo "Stopping serverone"
        kill $(ps aux | grep 'java' | grep 'ServerOneCLI' | awk '{print $2}')
        ;;
    *)
        echo "Usage: /etc/init.d/serverone.d start|stop"
        echo "Notes:"
        echo "  1. remember to set USER and ROOT"
        exit 1
        ;;
esac

exit 0

