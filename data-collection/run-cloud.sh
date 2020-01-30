#! /bin/bash

QUEUE_CLASS="refactoringml.RunQueue"
IMPORT_CLASS="refactoringml.RunImport"

JAR_PATH=target/refactoring-analyzer-0.0.1-SNAPSHOT-jar-with-dependencies.jar
REFACTORINGMINER_JAR_PATH=lib/RefactoringMiner-20190430.jar


if [ "$TASK" = "worker" ]
then

echo "Starting the worker in 3 minutes"
java -Xmx800m -Xms350m -cp $REFACTORINGMINER_JAR_PATH:$JAR_PATH $QUEUE_CLASS

else

echo "Starting the import in 2 minutes"
java -Xmx800m -Xms350m -cp $REFACTORINGMINER_JAR_PATH:$JAR_PATH $IMPORT_CLASS

fi
echo "Done!!"
