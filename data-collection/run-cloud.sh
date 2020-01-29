#! /bin/bash

export IFS=","

if [ "$#" -ne 8 ]; then
  echo "wrong usage" >&2
  exit 1
fi

CLASS="refactoringml.App"
JAR_PATH=predicting-refactoring-ml/data-collection/target/refactoring-analyzer-0.0.1-SNAPSHOT-jar-with-dependencies.jar
REFACTORINGMINER_JAR_PATH=predicting-refactoring-ml/data-collection/lib/RefactoringMiner-20190430.jar
OUTPUT_PATH=output
PROJECTS_CSV_PATH=$1
BEGIN=$2
END=$3
URL=$4
USER=$5
PWD=$6
STORAGE_MACHINE=$7
THRESHOLD=$8


mkdir $OUTPUT_PATH

echo ""
i=0
cat $PROJECTS_CSV_PATH | while
	read PROJECT REPO DATASET; do
	let "i++"

	if [ $i -ge $BEGIN -a $i -le $END ]; then
		echo "INIT $i = $PROJECT"
		echo "$i=$PROJECT" >> execution.txt

		OUTPUT_PROJECT_PATH="$OUTPUT_PATH/$PROJECT"
		mkdir $OUTPUT_PROJECT_PATH

		STORAGE_PATH="$OUTPUT_PROJECT_PATH/storage"
		mkdir $STORAGE_PATH

		echo "Running refactoring analyzer"

		java -Xmx800m -Xms350m -cp $REFACTORINGMINER_JAR_PATH:$JAR_PATH $CLASS $DATASET $REPO $STORAGE_PATH $URL $USER $PWD $THRESHOLD >> log.txt 2>> error.txt
		if [ $? -eq 0 ]
		then
			echo "Packing the java files"
			mv log.txt $OUTPUT_PROJECT_PATH
			mv error.txt $OUTPUT_PROJECT_PATH
			zip -q -r $DATASET-$PROJECT.zip $OUTPUT_PROJECT_PATH/*
			scp $DATASET-$PROJECT.zip $STORAGE_MACHINE/$DATASET-$PROJECT.zip
			rm $DATASET-$PROJECT.zip
		fi

		echo "Deleting folder"
		rm -rf $OUTPUT_PROJECT_PATH
		rm error.txt log.txt nohup.out

		echo ""
		echo "#####################"
		echo ""
	fi

done

