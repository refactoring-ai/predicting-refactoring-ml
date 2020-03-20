#! /bin/bash

#configuration
dir="data-collection"
logDir="logs/"
terminalLog="docker-terminal.log"
workerCount=40

#LOGIC:
cd data-collection
mkdir -p $logDir

#build the project
mvn -DskipTests clean compile package |&tee "${logDir}${terminalLog}"

#evaluate the build
buildResult=$(egrep -oh 'BUILD SUCCESS' "${logDir}${terminalLog}" | wc -l)

if [ $buildResult -eq 1 ]; then docker-compose up --scale worker=$workerCount |&tee -a "${logDir}${terminalLog}"
else echo "[FATAL] Failed to build the data-collection tool -> the data-collection will be aborted!"  |&tee -a "${logDir}${terminalLog}"
fi
