#! /bin/bash

dir="data-collection"
logDir="logs/"
terminalLog="docker-terminal.log"


cd data-collection
mkdir -p $logDir
mvn -DskipTests clean compile package
docker-compose up --scale worker=4 |&tee "${logDir}${terminalLog}"