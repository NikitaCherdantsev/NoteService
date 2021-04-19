#!/bin/sh
DIR="$(cd "$(dirname "$0")" >/dev/null 2>&1 && pwd)"
kill $(cat $DIR/RUNNING_PID)