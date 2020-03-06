#! /bin/bash

QUEUE_CLASS="refactoringml.RunQueue"
IMPORT_CLASS="refactoringml.RunImport"

JAR_PATH=target/refactoring-analyzer-0.0.1-SNAPSHOT-jar-with-dependencies.jar
REFACTORINGMINER_JAR_PATH=lib/RefactoringMiner-762e93ecafff48761eaa26e39626f43258781c4a.jar


if [ "$TASK" = "worker" ]
then

echo "Starting the worker soon"
java -Xmx800m -Xms350m -cp $REFACTORINGMINER_JAR_PATH:$JAR_PATH $QUEUE_CLASS

else

echo "Starting the import soon"
java -Xmx800m -Xms350m -cp $REFACTORINGMINER_JAR_PATH:$JAR_PATH $IMPORT_CLASS

fi
echo "Done!!"
