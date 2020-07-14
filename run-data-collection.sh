#! /bin/bash

#configuration
dir="data-collection"
logDir="logs/"
terminalLog="docker-terminal.log"

#read and validate 
if [ $# -eq 2 ]; then
	export FILE_TO_IMPORT=$1
	export Worker_Count=$2
elif [ $# -eq 5 ]; then
	export FILE_TO_IMPORT=$1
	export Worker_Count=$2
	export DB_URL=$3
	export DB_USER=$4
	export DB_PWD=$5
else 
	echo "[FATAL] Incorrect arguments! Please provide: [FILE_TO_IMPORT] [Worker_Count] (Optional): [DB_URL] [DB_USER] [DB_PWD]"
	exit 1
fi

#prepare data-collection
cd data-collection
mkdir -p $logDir

#build the project
#mvn -DskipTests clean compile package |&tee "${logDir}${terminalLog}"

#evaluate the build and start the data collection
buildResult=1 #$(egrep -oh 'BUILD SUCCESS' "${logDir}${terminalLog}" | wc -l)

if [ $buildResult -eq 1 ]; then 
	echo "Start docker-compose..."
	#optional maria db, e.g. for tests, the output is saved in the volumes/mysql folder
	if [ $# -eq 2 ]; then	
		docker-compose -f docker-compose.yml -f docker-compose_db.yml up --scale worker=$Worker_Count --force-recreate |&tee -a "${logDir}${terminalLog}"
	#custom db
	elif [ $# -eq 5 ]; then
		docker-compose up --scale worker=$Worker_Count --force-recreate |&tee -a "${logDir}${terminalLog}"
	fi	
else 
	echo "[FATAL] Failed to build the data-collection tool -> the data-collection will be aborted!"  |&tee -a "${logDir}${terminalLog}"
	exit 1
fi
