#!/bin/sh
DIR="$(cd "$(dirname "$0")" >/dev/null 2>&1 && pwd)"
nohup $DIR/bin/crude-service >>$DIR/application.log 2>&1 &
echo $! >$DIR/RUNNING_PID