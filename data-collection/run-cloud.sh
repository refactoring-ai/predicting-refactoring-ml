#! /bin/bash

QUEUE_CLASS="refactoringml.RunQueue"
IMPORT_CLASS="refactoringml.RunImport"

JAR_PATH=$(find -regextype posix-extended -regex '^.*SNAPSHOT-jar-with-dependencies.jar')
REFACTORINGMINER_JAR_PATH=$(find -regextype posix-extended -regex '^.*RefactoringMiner-[0-9a-zA-Z]*.jar')

if [ "$TASK" = "worker" ]
then

RC=1
counter=0
#not the correct exit code, but our programes always exists with 0
while [ $RC -ne 0 ]; do
  if [ $counter -eq 0 ]; then echo "Starting the queue class..."
  else echo "Restarting the queue class for the ${counter} time."
  fi

  java -Xmx800m -Xms350m -XX:+ExitOnOutOfMemoryError -cp $REFACTORINGMINER_JAR_PATH:$JAR_PATH $QUEUE_CLASS
  RC=$?
  counter=$((counter + 1))
done

else

echo "Starting the project import..."
java -Xmx800m -Xms350m -cp $REFACTORINGMINER_JAR_PATH:$JAR_PATH $IMPORT_CLASS

fi
echo "Done with task: ${TASK}!!"
