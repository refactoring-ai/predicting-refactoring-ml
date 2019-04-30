#! /bin/bash

export IFS=","

if [ "$#" -ne 8 ]; then
  echo "wrong usage" >&2
  exit 1
fi

CLASS="refactoringml.App"
JAR_PATH=$8/predicting-refactoring-ml/data-collection/target/refactoring-analyzer-0.0.1-SNAPSHOT-jar-with-dependencies.jar
REFACTORINGMINER_JAR_PATH=$8/predicting-refactoring-ml/data-collection/lib/RefactoringMiner-20190430.jar
OUTPUT_PATH=$8/output
PROJECTS_CSV_PATH=$1
BEGIN=$2
END=$3
URL=$4
USER=$5
PWD=$6
STORAGE_MACHINE=$7


mkdir $OUTPUT_PATH

echo ""
i=0
cat $PROJECTS_CSV_PATH | while 
	read PROJECT REPO DATASET; do
	let "i++"

	if [ $i -ge $BEGIN -a $i -le $END ]; then
		echo "INIT $PROJECT"

		OUTPUT_PROJECT_PATH="$OUTPUT_PATH/$PROJECT"
		mkdir $OUTPUT_PROJECT_PATH

		STORAGE_PATH="$OUTPUT_PROJECT_PATH/storage"
		mkdir $STORAGE_PATH

		echo "Running refactoring analyzer"

		java -Xmx650m -Xms350m -cp $REFACTORINGMINER_JAR_PATH:$JAR_PATH $CLASS $DATASET $REPO $STORAGE_PATH $URL $USER $PWD >> $8/log.txt 2>> $8/error.txt
		if [ $? -eq 0 ]
		then
			echo "Zipping and sending it to the storage machine"
			zip -q -r $DATASET-$PROJECT.zip $OUTPUT_PROJECT_PATH/*
			scp $DATASET-$PROJECT.zip $STORAGE_MACHINE/$DATASET-$PROJECT.zip
			rm $DATASET-$PROJECT.zip
		fi

		echo "Deleting folder"
		rm -rf $OUTPUT_PROJECT_PATH

		echo ""
		echo "#####################"
		echo ""
	fi

done

