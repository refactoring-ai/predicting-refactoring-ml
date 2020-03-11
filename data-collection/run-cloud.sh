#! /bin/bash

QUEUE_CLASS="refactoringml.RunQueue"
IMPORT_CLASS="refactoringml.RunImport"

JAR_PATH=target/refactoring-analyzer-0.0.1-SNAPSHOT-jar-with-dependencies.jar
REFACTORINGMINER_JAR_PATH=lib/RefactoringMiner-762e93ecafff48761eaa26e39626f43258781c4a.jar

if [ "$TASK" = "worker" ]
then

echo "Starting the worker soon"
RC=1
counter=0
#not the correct exit code, but our programes always exists with 0
while [ $RC -ne -1 ]; do
  if [ $counter -eq 0 ]; then echo "Starting the queue class."
  else echo "Restarting the queue class for the ${counter} time."
  fi

  java -Xmx800m -Xms350m -cp $REFACTORINGMINER_JAR_PATH:$JAR_PATH $QUEUE_CLASS
  RC=$?
  counter=$((counter + 1))
done

else

echo "Starting the import soon"
java -Xmx800m -Xms350m -cp $REFACTORINGMINER_JAR_PATH:$JAR_PATH $IMPORT_CLASS

fi
echo "Done with task: ${TASK}!!"