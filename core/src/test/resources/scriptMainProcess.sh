#!/bin/bash
SCRIPT_PATH=$1
echo "Running subprocess scriptSubProcess.sh Pid=$$"
/bin/sh $SCRIPT_PATH/scriptLoop.sh "-Djcontainer.id=1234"
