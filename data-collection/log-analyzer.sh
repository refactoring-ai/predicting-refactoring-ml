#! /bin/bash

infoFile="logs/data-collection_INFO.log"
errorFile="logs/data-collection_ERROR.log"
outFile="statistics.txt"

#General Statistics
echo "------------General Statistics------------" > $outFile
totalTime=$(grep -o '.git in [0-9]*.[0-9]*' $infoFile | grep -Eo '[+-]?[0-9]+([.][0-9]+)?' | awk '{s+=$1} END {print s}')
totalProjects=$(grep 'refactoring- and ' $infoFile | wc -l)
echo "A total of ${totalProjects} projects were processed with a total App executiom time of ${totalTime} minutes." >> $outFile
totalRefactorings=$(grep 'refactoring- and ' $infoFile | sed -e 's/Found \(.*\) refactoring-.*/\1/' | awk '{s+=$1} END {print s}')
echo "${totalRefactorings} refactoring instances were found in total." >> $outFile
totalStableInstances=$(grep 'refactoring- and' $infoFile | sed -e 's/Found [0-9]* refactoring- and \(.*\) stable.*/\1/' | awk '{s+=$1} END {print s}')
echo "${totalStableInstances} stable instances were found in total." >> $outFile

for ((i=20;i<=50;i+=10)); do
	currentStable=$(grep -A 5 'refactoring- and ' $infoFile | echo "with threshold: ${i}" | grep select | sed -e 's/Found \(.*\) stable.*/\1/')
	echo -e "\t${currentStable} stablecommit instances were found for threshold ${i}" >> $outFile
done
echo -e "\n" >> $outFile

#Exceptions
echo "------------Exceptions------------" >> $outFile
exceptionCount=$(grep 'Exception:' $errorFile |wc -l)
uniqueExceptions=$(grep 'Exception:' $errorFile | sed 's/: .*//' | sort --unique)
uniqueExceptionsCount=$(grep 'Exception:' $errorFile | sed 's/: .*//' | sort --unique | wc -l)
echo "${exceptionCount} exceptions occured during runtime, ${uniqueExceptionsCount} of these exceptions were unique" >> $outFile

for e in $uniqueExceptions; do
	currentExceptionCount=$(echo "${e}" | grep select $errorFile | wc -l)
	echo -e "\t${currentExceptionCount} ${e}s occured during runtime." >> $outFile
done
echo -e "\n" >> $outFile

#Individual project results
echo "------------Individual Project Results------------" >> $outFile
grep -A 5 'refactoring- and ' $infoFile >> $outFile
echo -e "\n" >> $outFile
