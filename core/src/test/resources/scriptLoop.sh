#!/bin/bash
function clean_up {
	echo "Terminate process"
	exit 0
}

trap clean_up SIGHUP SIGINT SIGTERM

echo "Sleeping.  Pid=$$"
while :
do
	sleep 1 &
	wait
	echo "Sleep over"
done
