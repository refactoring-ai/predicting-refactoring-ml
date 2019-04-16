#!/usr/bin/env bash
export IFS=","

if [ "$#" -ne 7 ]; then
  echo "Usage: $0 CSV BEGIN END DATASET_NAME DB_URL DB_USER DB_PWD" >&2
  exit 1
fi

JAR_PATH=/root/predicting-refactoring-ml/data-collection/target/refactoring-analyzer-0.0.1-SNAPSHOT-jar-with-dependencies.jar
REFACTORINGMINER_JAR_PATH=/root/predicting-refactoring-ml/data-collection/lib/RefactoringMiner-3.jar
OUTPUT_PATH=/root/output
PROJECTS_CSV_PATH=$1
ASTCONVERTER=/root/predicting-refactoring-ml/data-collection/astconverter/astconverter.jar
ASTCONVERTER2=/root/predicting-refactoring-ml/data-collection/astconverter/astconverter2.jar
THRESHOLD=1000
BEGIN=$2
END=$3
DATASET=$4
URL=$5
USER=$6
PWD=$7


CLASS="refactoringml.App"

mkdir $OUTPUT_PATH
echo ""
i=0
cat $PROJECTS_CSV_PATH | while 
	read PROJECT REPO; do 
	let "i++"

	if [ $i -ge $BEGIN -a $i -le $END ]; then
		echo "INIT $PROJECT"

		OUTPUT_PROJECT_PATH="$OUTPUT_PATH/$PROJECT"
		mkdir $OUTPUT_PROJECT_PATH

		echo "Running refactoring analyzer"
		echo "java -cp $ANICHE_JAR_PATH:$JAR_PATH $CLASS $DATASET $REPO $OUTPUT_PROJECT_PATH $THRESHOLD"

		java -Xmx650m -Xms350m -cp $REFACTORINGMINER_JAR_PATH:$JAR_PATH $CLASS $DATASET $REPO $OUTPUT_PROJECT_PATH $THRESHOLD $URL $USER $PWD >> /root/log.txt 2>> /root/error.txt
		if [ $? -eq 0 ]
		then
			AST_OUTPUT_PATH="$OUTPUT_PROJECT_PATH/clean"
			mkdir $AST_OUTPUT_PATH
			mkdir $AST_OUTPUT_PATH/astconverter1
			echo "Running astconverter1"
			java -jar $ASTCONVERTER -aIn $OUTPUT_PROJECT_PATH/storage -aOut $AST_OUTPUT_PATH/astconverter1
			echo "Running astconverter2"
			mkdir $AST_OUTPUT_PATH/astconverter2
			java -jar $ASTCONVERTER2 -aIn $OUTPUT_PROJECT_PATH/storage -aOut $AST_OUTPUT_PATH/astconverter2

			echo "Zipping"
			zip -q -r $PROJECT.zip $OUTPUT_PROJECT_PATH/*
		fi

		echo "Deleting folder"
		rm -rf $OUTPUT_PROJECT_PATH

		echo ""
		echo "#####################"
		echo ""
	fi

done

