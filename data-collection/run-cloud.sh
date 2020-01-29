#! /bin/bash

CLASS="refactoringml.RunQueue"
JAR_PATH=target/refactoring-analyzer-0.0.1-SNAPSHOT-jar-with-dependencies.jar
REFACTORINGMINER_JAR_PATH=lib/RefactoringMiner-20190430.jar

java -Xmx800m -Xms350m -cp $REFACTORINGMINER_JAR_PATH:$JAR_PATH $CLASS

echo "Done!!"
